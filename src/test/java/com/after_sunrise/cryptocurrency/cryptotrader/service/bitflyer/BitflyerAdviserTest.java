package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ProductType;
import org.apache.commons.configuration2.MapConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.AbstractMap.SimpleEntry;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ProductType.*;
import static java.math.BigDecimal.*;
import static java.math.BigDecimal.valueOf;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitflyerAdviserTest {

    private BitflyerAdviser target;

    private TestModule module;

    private Context context;

    private Map<String, Object> configurations;

    @BeforeMethod
    public void setUp() throws Exception {

        configurations = new HashMap<>();

        module = new TestModule();

        context = module.getMock(Context.class);

        target = spy(new BitflyerAdviser());

        target.setConfiguration(new MapConfiguration(configurations));

        when(context.roundLotSize(any(), any(), any())).thenAnswer(i -> {

            BigDecimal value = i.getArgumentAt(1, BigDecimal.class);

            RoundingMode mode = i.getArgumentAt(2, RoundingMode.class);

            if (value == null || mode == null) {
                return null;
            }

            BigDecimal unit = new BigDecimal("0.5");

            BigDecimal units = value.divide(unit, INTEGER_ZERO, mode);

            return units.multiply(unit);

        });

    }

    @Test
    public void testGet() {
        assertEquals(target.get(), BitflyerService.ID);
    }

    @Test
    public void testCalculateSwapRate() {

        ZoneId zone = ZoneId.of("Asia/Tokyo");
        LocalDateTime zdt = LocalDateTime.of(2017, 8, 25, 8, 0);
        Instant now = ZonedDateTime.of(zdt, zone).toInstant();

        Request.RequestBuilder b = Request.builder().currentTime(now);

        // SD (No Swap)
        LocalDateTime exp = LocalDateTime.of(2017, 8, 25, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateSwapRate(context, b.build()), ZERO);

        // Past (No Swap)
        exp = LocalDateTime.of(2017, 8, 24, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateSwapRate(context, b.build()), ZERO);

        // S+1
        exp = LocalDateTime.of(2017, 8, 26, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateSwapRate(context, b.build()), new BigDecimal("0.0004000000"));

        // S+7
        exp = LocalDateTime.of(2017, 9, 1, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateSwapRate(context, b.build()), new BigDecimal("0.0028033623"));

        // S+14
        exp = LocalDateTime.of(2017, 9, 8, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateSwapRate(context, b.build()), new BigDecimal("0.0056145834"));

        // Null current time
        assertEquals(target.calculateSwapRate(context, Request.builder().build()), ZERO);

        // Null Expiry
        when(context.getExpiry(Key.from(b.build()))).thenReturn(null);
        assertEquals(target.calculateSwapRate(context, b.build()), ZERO);

    }

    @Test
    public void testAdjustBasis() {

        Request request = Request.builder().build();

        doReturn(new BigDecimal("0.0005")).when(target).calculateSwapRate(context, request);

        BigDecimal result = target.adjustBasis(context, request, new BigDecimal("0.002"));

        assertEquals(result, new BigDecimal("0.0025"));

        assertNull(target.adjustBasis(context, request, null));

    }

    @Test
    public void testGetUnderlyingKey() {

        Request.RequestBuilder builder = Request.builder().site("bf").currentTime(Instant.now());

        // Null instrument
        Key key = target.getUnderlyingKey(builder.build());
        assertNull(key);

        for (ProductType p : ProductType.values()) {

            key = target.getUnderlyingKey(builder.instrument(p.name()).build());

            switch (p) {
                case BTCJPY_MAT1WK:
                case BTCJPY_MAT2WK:
                    assertEquals(key.getSite(), builder.build().getSite());
                    assertEquals(key.getInstrument(), "BTC_JPY");
                    assertEquals(key.getTimestamp(), builder.build().getCurrentTime());
                    break;
                default:
                    assertNull(key, p.name());
                    break;
            }

        }

    }

    @Test
    public void testAdjustBuyBoundaryPrice() {

        Request request = Request.builder().site("s").instrument("i")
                .tradingSpread(new BigDecimal("0.0008")).build();
        Key key = Key.from(request);

        doReturn(key).when(target).getUnderlyingKey(request);
        doReturn(new BigDecimal("0.0004")).when(target).calculateSwapRate(context, request);
        when(context.getCommissionRate(key)).thenReturn(new BigDecimal("0.0012"));
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("5000"));

        // Passive than market
        BigDecimal result = target.adjustBuyBoundaryPrice(context, request, new BigDecimal("6000"));
        assertEquals(result, new BigDecimal("4988.0000"));

        // Aggressive than Market
        result = target.adjustBuyBoundaryPrice(context, request, new BigDecimal("4000"));
        assertEquals(result, new BigDecimal("4000"));

        // Null Market
        result = target.adjustBuyBoundaryPrice(context, request, null);
        assertEquals(result, null);

    }

    @Test
    public void testAdjustSellBoundaryPrice() {

        Request request = Request.builder().site("s").instrument("i")
                .tradingSpread(new BigDecimal("0.0008")).build();
        Key key = Key.from(request);

        doReturn(key).when(target).getUnderlyingKey(request);
        doReturn(new BigDecimal("0.0004")).when(target).calculateSwapRate(context, request);
        when(context.getCommissionRate(key)).thenReturn(new BigDecimal("0.0012"));
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("5000"));

        // Passive than market
        BigDecimal result = target.adjustSellBoundaryPrice(context, request, new BigDecimal("4000"));
        assertEquals(result, new BigDecimal("5012.0000"));

        // Aggressive than Market
        result = target.adjustSellBoundaryPrice(context, request, new BigDecimal("6000"));
        assertEquals(result, new BigDecimal("6000"));

        // Null Market
        result = target.adjustSellBoundaryPrice(context, request, null);
        assertEquals(result, null);

    }

    @Test
    public void testAdjustFundingOffset() {

        configurations.put(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerAdviser.products.offset",
                "BTC_JPY:bitflyer:BTCJPY_MAT1WK"
        );

        BigDecimal offset = new BigDecimal("0.0000");
        Request r1 = Request.builder().instrument(BTC_JPY.name()).build();
        Request r2 = Request.builder().site(BitflyerService.ID).instrument(BTCJPY_MAT1WK.name()).build();

        Stream.of(
                new SimpleEntry<>(new BigDecimal("845000"), new BigDecimal("40.0000000000")), // +30%
                new SimpleEntry<>(new BigDecimal("780000"), new BigDecimal("40.0000000000")), // +20%
                new SimpleEntry<>(new BigDecimal("715000"), new BigDecimal("20.0000000000")), // +10%
                new SimpleEntry<>(new BigDecimal("663000"), new BigDecimal("4.0000000000")), // +2%
                new SimpleEntry<>(new BigDecimal("656500"), new BigDecimal("2.0000000000")), // +1%
                new SimpleEntry<>(new BigDecimal("650000"), new BigDecimal("0.0000000000")), // 0%
                new SimpleEntry<>(new BigDecimal("643500"), new BigDecimal("-0.1000000000")), // -1%
                new SimpleEntry<>(new BigDecimal("637000"), new BigDecimal("-0.2000000000")), // -2%
                new SimpleEntry<>(new BigDecimal("585000"), new BigDecimal("-1.0000000000")), // -10%
                new SimpleEntry<>(new BigDecimal("520000"), new BigDecimal("-2.0000000000")), // -20%
                new SimpleEntry<>(new BigDecimal("455000"), new BigDecimal("-2.0000000000")) // -30%
        ).forEach(e -> {
            when(context.getMidPrice(Key.from(r1))).thenReturn(BigDecimal.valueOf(650000));
            when(context.getMidPrice(Key.from(r2))).thenReturn(e.getKey());
            assertEquals(target.adjustFundingOffset(context, r1, offset), e.getValue(), e.getKey().toPlainString());
            assertEquals(target.adjustFundingOffset(context, r2, offset), offset);
        });

        // Zero 1
        when(context.getMidPrice(Key.from(r1))).thenReturn(new BigDecimal("0.0"));
        when(context.getMidPrice(Key.from(r2))).thenReturn(BigDecimal.valueOf(650000));
        assertEquals(target.adjustFundingOffset(context, r1, offset), offset);
        assertEquals(target.adjustFundingOffset(context, r2, offset), offset);

        // Zero 2
        when(context.getMidPrice(Key.from(r1))).thenReturn(BigDecimal.valueOf(650000));
        when(context.getMidPrice(Key.from(r2))).thenReturn(new BigDecimal("0.0"));
        assertEquals(target.adjustFundingOffset(context, r1, offset), offset);
        assertEquals(target.adjustFundingOffset(context, r2, offset), offset);

        // Null 1
        when(context.getMidPrice(Key.from(r1))).thenReturn(null);
        when(context.getMidPrice(Key.from(r2))).thenReturn(BigDecimal.valueOf(650000));
        assertEquals(target.adjustFundingOffset(context, r1, offset), offset);
        assertEquals(target.adjustFundingOffset(context, r2, offset), offset);

        // Null 2
        when(context.getMidPrice(Key.from(r1))).thenReturn(BigDecimal.valueOf(650000));
        when(context.getMidPrice(Key.from(r2))).thenReturn(null);
        assertEquals(target.adjustFundingOffset(context, r1, offset), offset);
        assertEquals(target.adjustFundingOffset(context, r2, offset), offset);

    }

    @Test
    public void testGetEquivalentSize() {

        Request request1 = Request.builder().instrument(FX_BTC_JPY.name()).build();
        Request request2 = Request.builder().instrument(ETH_BTC.name()).build();
        when(context.getInstrumentPosition(Key.from(request1))).thenReturn(TEN);

        // Mid * Size
        when(context.getMidPrice(Key.from(request2))).thenReturn(new BigDecimal("0.0765"));
        when(context.getLastPrice(Key.from(request2))).thenReturn(new BigDecimal("0.0789"));
        when(context.getInstrumentPosition(Key.from(request2))).thenReturn(new BigDecimal("5.5"));
        assertEquals(target.getEquivalentSize(context, request1, ETH_BTC), new BigDecimal("0.42075"));

        // Last * Size
        when(context.getMidPrice(Key.from(request2))).thenReturn(null);
        when(context.getLastPrice(Key.from(request2))).thenReturn(new BigDecimal("0.0789"));
        when(context.getInstrumentPosition(Key.from(request2))).thenReturn(new BigDecimal("5.5"));
        assertEquals(target.getEquivalentSize(context, request1, ETH_BTC), new BigDecimal("0.43395"));

        // Null price
        when(context.getMidPrice(Key.from(request2))).thenReturn(null);
        when(context.getLastPrice(Key.from(request2))).thenReturn(null);
        when(context.getInstrumentPosition(Key.from(request2))).thenReturn(new BigDecimal("5.5"));
        assertEquals(target.getEquivalentSize(context, request1, ETH_BTC), null);

        // Null price + Flat position
        when(context.getMidPrice(Key.from(request2))).thenReturn(null);
        when(context.getLastPrice(Key.from(request2))).thenReturn(null);
        when(context.getInstrumentPosition(Key.from(request2))).thenReturn(new BigDecimal("0.00"));
        assertEquals(target.getEquivalentSize(context, request1, ETH_BTC), ZERO);

        // Null price + Null Position
        when(context.getMidPrice(Key.from(request2))).thenReturn(null);
        when(context.getLastPrice(Key.from(request2))).thenReturn(null);
        when(context.getInstrumentPosition(Key.from(request2))).thenReturn(null);
        assertEquals(target.getEquivalentSize(context, request1, ETH_BTC), null);

        // No conversion
        assertEquals(target.getEquivalentSize(context, request1, FX_BTC_JPY), TEN);

    }

    @Test
    public void testGetHedgeSize() {

        Request request = Request.builder().instrument(FX_BTC_JPY.name()).build();
        Key key = Key.from(request);
        Set<ProductType> products = EnumSet.of(FX_BTC_JPY, BTCJPY_MAT1WK, BTCJPY_MAT2WK);

        //
        // Zero hedged
        //
        when(context.getInstrumentPosition(key)).thenReturn(valueOf(0));

        doReturn(valueOf(0)).when(target).getEquivalentSize(context, request, BTCJPY_MAT1WK);
        doReturn(valueOf(0)).when(target).getEquivalentSize(context, request, BTCJPY_MAT2WK);
        assertEquals(target.getHedgeSize(context, request, products), valueOf(0));

        doReturn(valueOf(-1)).when(target).getEquivalentSize(context, request, BTCJPY_MAT1WK);
        doReturn(valueOf(+1)).when(target).getEquivalentSize(context, request, BTCJPY_MAT2WK);
        assertEquals(target.getHedgeSize(context, request, products), valueOf(0));

        doReturn(valueOf(-2)).when(target).getEquivalentSize(context, request, BTCJPY_MAT1WK);
        doReturn(valueOf(+1)).when(target).getEquivalentSize(context, request, BTCJPY_MAT2WK);
        assertEquals(target.getHedgeSize(context, request, products), valueOf(+1));

        doReturn(valueOf(-1)).when(target).getEquivalentSize(context, request, BTCJPY_MAT1WK);
        doReturn(valueOf(+2)).when(target).getEquivalentSize(context, request, BTCJPY_MAT2WK);
        assertEquals(target.getHedgeSize(context, request, products), valueOf(-1));

        //
        // Long hedged
        //
        when(context.getInstrumentPosition(key)).thenReturn(valueOf(+1));

        doReturn(valueOf(0)).when(target).getEquivalentSize(context, request, BTCJPY_MAT1WK);
        doReturn(valueOf(0)).when(target).getEquivalentSize(context, request, BTCJPY_MAT2WK);
        assertEquals(target.getHedgeSize(context, request, products), valueOf(-1));

        doReturn(valueOf(-1)).when(target).getEquivalentSize(context, request, BTCJPY_MAT1WK);
        doReturn(valueOf(+1)).when(target).getEquivalentSize(context, request, BTCJPY_MAT2WK);
        assertEquals(target.getHedgeSize(context, request, products), valueOf(-1));

        doReturn(valueOf(-2)).when(target).getEquivalentSize(context, request, BTCJPY_MAT1WK);
        doReturn(valueOf(+1)).when(target).getEquivalentSize(context, request, BTCJPY_MAT2WK);
        assertEquals(target.getHedgeSize(context, request, products), valueOf(0));

        doReturn(valueOf(-1)).when(target).getEquivalentSize(context, request, BTCJPY_MAT1WK);
        doReturn(valueOf(+2)).when(target).getEquivalentSize(context, request, BTCJPY_MAT2WK);
        assertEquals(target.getHedgeSize(context, request, products), valueOf(-2));

        //
        // Short hedged
        //
        when(context.getInstrumentPosition(key)).thenReturn(valueOf(-1));

        doReturn(valueOf(0)).when(target).getEquivalentSize(context, request, BTCJPY_MAT1WK);
        doReturn(valueOf(0)).when(target).getEquivalentSize(context, request, BTCJPY_MAT2WK);
        assertEquals(target.getHedgeSize(context, request, products), valueOf(+1));

        doReturn(valueOf(-1)).when(target).getEquivalentSize(context, request, BTCJPY_MAT1WK);
        doReturn(valueOf(+1)).when(target).getEquivalentSize(context, request, BTCJPY_MAT2WK);
        assertEquals(target.getHedgeSize(context, request, products), valueOf(+1));

        doReturn(valueOf(-2)).when(target).getEquivalentSize(context, request, BTCJPY_MAT1WK);
        doReturn(valueOf(+1)).when(target).getEquivalentSize(context, request, BTCJPY_MAT2WK);
        assertEquals(target.getHedgeSize(context, request, products), valueOf(2));

        doReturn(valueOf(-1)).when(target).getEquivalentSize(context, request, BTCJPY_MAT1WK);
        doReturn(valueOf(+2)).when(target).getEquivalentSize(context, request, BTCJPY_MAT2WK);
        assertEquals(target.getHedgeSize(context, request, products), valueOf(0));

        //
        // Null hedged
        //
        when(context.getInstrumentPosition(key)).thenReturn(null);
        doReturn(valueOf(0)).when(target).getEquivalentSize(context, request, BTCJPY_MAT1WK);
        doReturn(valueOf(0)).when(target).getEquivalentSize(context, request, BTCJPY_MAT2WK);
        assertEquals(target.getHedgeSize(context, request, products), null);

        when(context.getInstrumentPosition(key)).thenReturn(valueOf(0));
        doReturn(null).when(target).getEquivalentSize(context, request, BTCJPY_MAT1WK);
        doReturn(valueOf(0)).when(target).getEquivalentSize(context, request, BTCJPY_MAT2WK);
        assertEquals(target.getHedgeSize(context, request, products), null);

        when(context.getInstrumentPosition(key)).thenReturn(valueOf(0));
        doReturn(valueOf(0)).when(target).getEquivalentSize(context, request, BTCJPY_MAT1WK);
        doReturn(null).when(target).getEquivalentSize(context, request, BTCJPY_MAT2WK);
        assertEquals(target.getHedgeSize(context, request, products), null);

    }

    @Test
    public void testAdjustBuyLimitSize() {

        configurations.put(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerAdviser.products.hedge",
                "BTCJPY_MAT1WK"
        );

        BigDecimal size = new BigDecimal("123.45");
        BigDecimal exposure = new BigDecimal("0.45");
        Request request1 = Request.builder().instrument(BTCJPY_MAT1WK.name()).tradingExposure(exposure).build();
        Request request2 = Request.builder().instrument(BTC_JPY.name()).build();

        // 0
        doReturn(valueOf(0)).when(target).getHedgeSize(any(), any(), any());
        assertEquals(target.adjustBuyLimitSize(context, request1, size), new BigDecimal("0.0"));
        assertEquals(target.adjustBuyLimitSize(context, request2, size), size);

        // 1 * 0.45 = 0.45 -> 0.5
        doReturn(valueOf(+1)).when(target).getHedgeSize(any(), any(), any());
        assertEquals(target.adjustBuyLimitSize(context, request1, size), new BigDecimal("0.5"));
        assertEquals(target.adjustBuyLimitSize(context, request2, size), size);

        // 6 * 0.45 = 2.70 -> 2.5
        doReturn(valueOf(+6)).when(target).getHedgeSize(any(), any(), any());
        assertEquals(target.adjustBuyLimitSize(context, request1, size), new BigDecimal("2.5"));
        assertEquals(target.adjustBuyLimitSize(context, request2, size), size);

        // No hedge
        doReturn(valueOf(-1)).when(target).getHedgeSize(any(), any(), any());
        assertEquals(target.adjustBuyLimitSize(context, request1, size), new BigDecimal("0.0"));
        assertEquals(target.adjustBuyLimitSize(context, request2, size), size);

        // Null Hedge
        doReturn(null).when(target).getHedgeSize(any(), any(), any());
        assertEquals(target.adjustBuyLimitSize(context, request1, size), ZERO);
        assertEquals(target.adjustBuyLimitSize(context, request2, size), size);

    }

    @Test
    public void testAdjustSellLimitSize() {

        configurations.put(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerAdviser.products.hedge",
                "BTCJPY_MAT1WK"
        );

        BigDecimal size = new BigDecimal("123.45");
        BigDecimal exposure = new BigDecimal("0.45");
        Request request1 = Request.builder().instrument(BTCJPY_MAT1WK.name()).tradingExposure(exposure).build();
        Request request2 = Request.builder().instrument(BTC_JPY.name()).build();

        // 0
        doReturn(valueOf(0)).when(target).getHedgeSize(any(), any(), any());
        assertEquals(target.adjustSellLimitSize(context, request1, size), new BigDecimal("0.0"));
        assertEquals(target.adjustSellLimitSize(context, request2, size), size);

        // -1 * 0.45 = 0.45 -> 0.5
        doReturn(valueOf(-1)).when(target).getHedgeSize(any(), any(), any());
        assertEquals(target.adjustSellLimitSize(context, request1, size), new BigDecimal("0.5"));
        assertEquals(target.adjustSellLimitSize(context, request2, size), size);

        // -6 * 0.45 = 2.70 -> 2.5
        doReturn(valueOf(-6)).when(target).getHedgeSize(any(), any(), any());
        assertEquals(target.adjustSellLimitSize(context, request1, size), new BigDecimal("2.5"));
        assertEquals(target.adjustSellLimitSize(context, request2, size), size);

        // No hedge
        doReturn(valueOf(+1)).when(target).getHedgeSize(any(), any(), any());
        assertEquals(target.adjustSellLimitSize(context, request1, size), new BigDecimal("0.0"));
        assertEquals(target.adjustSellLimitSize(context, request2, size), size);

        // Null Hedge
        doReturn(null).when(target).getHedgeSize(any(), any(), any());
        assertEquals(target.adjustSellLimitSize(context, request1, size), ZERO);
        assertEquals(target.adjustSellLimitSize(context, request2, size), size);

    }

}
