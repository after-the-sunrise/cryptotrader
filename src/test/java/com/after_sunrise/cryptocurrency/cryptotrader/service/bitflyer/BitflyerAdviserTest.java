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
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ID;
import static java.math.BigDecimal.ZERO;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

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
        assertEquals(target.get(), ID);
    }

    @Test
    public void testCalculateSwapRate() {

        ZoneId zone = ZoneId.of("Asia/Tokyo");
        LocalDateTime zdt = LocalDateTime.of(2017, 8, 25, 8, 0);
        Instant now = ZonedDateTime.of(zdt, zone).toInstant();
        BigDecimal rate = new BigDecimal("0.0004");

        Request.RequestBuilder b = Request.builder().currentTime(now);

        // SD (No Swap)
        LocalDateTime exp = LocalDateTime.of(2017, 8, 25, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateSwapRate(context, b.build(), rate), ZERO);

        // Past (No Swap)
        exp = LocalDateTime.of(2017, 8, 24, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateSwapRate(context, b.build(), rate), ZERO);

        // S+1
        exp = LocalDateTime.of(2017, 8, 26, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateSwapRate(context, b.build(), rate), new BigDecimal("0.0004000000"));

        // S+7
        exp = LocalDateTime.of(2017, 9, 1, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateSwapRate(context, b.build(), rate), new BigDecimal("0.0028033623"));

        // S+14
        exp = LocalDateTime.of(2017, 9, 8, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateSwapRate(context, b.build(), rate), new BigDecimal("0.0056145834"));

        // S+14 @ x2 bps
        rate = rate.add(rate);
        exp = LocalDateTime.of(2017, 9, 8, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateSwapRate(context, b.build(), rate), new BigDecimal("0.0112584268"));

        // Null rate
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateSwapRate(context, b.build(), null), ZERO);

        // Null current time
        assertEquals(target.calculateSwapRate(context, Request.builder().build(), rate), ZERO);

        // Null Expiry
        when(context.getExpiry(Key.from(b.build()))).thenReturn(null);
        assertEquals(target.calculateSwapRate(context, b.build(), rate), ZERO);

    }

    @Test
    public void testGetSfdTable() {

        // Default
        NavigableMap<BigDecimal, BigDecimal> result = target.getSfdTable();
        assertEquals(result.size(), 3, result.toString());
        assertEquals(result.get(new BigDecimal("0.1000")), new BigDecimal("0.0050"));
        assertEquals(result.get(new BigDecimal("0.1500")), new BigDecimal("0.0100"));
        assertEquals(result.get(new BigDecimal("0.2000")), new BigDecimal("0.0300"));

        // Cached
        assertSame(target.getSfdTable(), result);
        assertSame(target.getSfdTable(), result);
        assertSame(target.getSfdTable(), result);

        // Override
        configurations.put(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerAdviser.sfd.tbl",
                "0.01:0.02|0.03:0.04|"
        );
        result = target.getSfdTable();
        assertEquals(result.size(), 2, result.toString());
        assertEquals(result.get(new BigDecimal("0.01")), new BigDecimal("0.02"));
        assertEquals(result.get(new BigDecimal("0.03")), new BigDecimal("0.04"));

        // Cached
        assertSame(target.getSfdTable(), result);
        assertSame(target.getSfdTable(), result);

    }

    @Test
    public void testCalculateSfdRate() {

        Instant now = Instant.now();
        Key cKey = Key.builder().site("bf").timestamp(now).instrument("BTC_JPY").build();
        Key fKey = Key.builder().site("bf").timestamp(now).instrument("FX_BTC_JPY").build();

        Request r1 = Request.builder().site("bf").currentTime(now).instrument("FX_BTC_JPY").build();
        Request r2 = Request.builder().site("bf").currentTime(now).instrument("ETH_BTC").build();
        when(context.getLastPrice(cKey)).thenReturn(new BigDecimal("2000000"));

        //
        // Buy
        //

        // Zero SFD
        when(context.getBestAskPrice(fKey)).thenReturn(new BigDecimal("1900000"));
        assertEquals(target.calculateSfdRate(context, r1, true), new BigDecimal("0"));

        // Tier 1
        when(context.getBestAskPrice(fKey)).thenReturn(new BigDecimal("2200000"));
        assertEquals(target.calculateSfdRate(context, r1, true), new BigDecimal("0.0050"));

        // Tier 1+
        when(context.getBestAskPrice(fKey)).thenReturn(new BigDecimal("2240000"));
        assertEquals(target.calculateSfdRate(context, r1, true), new BigDecimal("0.0050"));

        // Tier 2
        when(context.getBestAskPrice(fKey)).thenReturn(new BigDecimal("2300000"));
        assertEquals(target.calculateSfdRate(context, r1, true), new BigDecimal("0.0100"));

        // Tier 2+
        when(context.getBestAskPrice(fKey)).thenReturn(new BigDecimal("2360000"));
        assertEquals(target.calculateSfdRate(context, r1, true), new BigDecimal("0.0100"));

        // Tier 3
        when(context.getBestAskPrice(fKey)).thenReturn(new BigDecimal("2400000"));
        assertEquals(target.calculateSfdRate(context, r1, true), new BigDecimal("0.0300"));

        // Tier 3+
        when(context.getBestAskPrice(fKey)).thenReturn(new BigDecimal("2440000"));
        assertEquals(target.calculateSfdRate(context, r1, true), new BigDecimal("0.0300"));

        // Not FX
        assertEquals(target.calculateSfdRate(context, r2, true), ZERO);

        // Null Price
        when(context.getBestAskPrice(fKey)).thenReturn(null);
        assertEquals(target.calculateSfdRate(context, r1, true), ZERO);

        //
        // Sell
        //

        // Zero SFD
        when(context.getBestBidPrice(fKey)).thenReturn(new BigDecimal("2100000"));
        assertEquals(target.calculateSfdRate(context, r1, false), new BigDecimal("0"));

        // Tier 1
        when(context.getBestBidPrice(fKey)).thenReturn(new BigDecimal("1800000"));
        assertEquals(target.calculateSfdRate(context, r1, false), new BigDecimal("0.0050"));

        // Tier 1+
        when(context.getBestBidPrice(fKey)).thenReturn(new BigDecimal("1740000"));
        assertEquals(target.calculateSfdRate(context, r1, false), new BigDecimal("0.0050"));

        // Tier 2
        when(context.getBestBidPrice(fKey)).thenReturn(new BigDecimal("1700000"));
        assertEquals(target.calculateSfdRate(context, r1, false), new BigDecimal("0.0100"));

        // Tier 2+
        when(context.getBestBidPrice(fKey)).thenReturn(new BigDecimal("1660000"));
        assertEquals(target.calculateSfdRate(context, r1, false), new BigDecimal("0.0100"));

        // Tier 3
        when(context.getBestBidPrice(fKey)).thenReturn(new BigDecimal("1600000"));
        assertEquals(target.calculateSfdRate(context, r1, false), new BigDecimal("0.0300"));

        // Tier 3
        when(context.getBestBidPrice(fKey)).thenReturn(new BigDecimal("1540000"));
        assertEquals(target.calculateSfdRate(context, r1, false), new BigDecimal("0.0300"));

        // Not FX
        assertEquals(target.calculateSfdRate(context, r2, false), ZERO);

        // Null Price
        when(context.getBestBidPrice(fKey)).thenReturn(null);
        assertEquals(target.calculateSfdRate(context, r1, false), ZERO);

    }

    @Test
    public void testCalculateSfdRate_Interpolated() {

        Instant now = Instant.now();
        Key cKey = Key.builder().site("bf").timestamp(now).instrument("BTC_JPY").build();
        Key fKey = Key.builder().site("bf").timestamp(now).instrument("FX_BTC_JPY").build();

        Request r1 = Request.builder().site("bf").currentTime(now).instrument("FX_BTC_JPY").build();
        Request r2 = Request.builder().site("bf").currentTime(now).instrument("ETH_BTC").build();
        when(context.getLastPrice(cKey)).thenReturn(new BigDecimal("2000000"));

        configurations.put(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerAdviser.sfd.pct",
                new BigDecimal("-1.01")
        );

        //
        // Buy
        //

        // Zero SFD
        when(context.getBestAskPrice(fKey)).thenReturn(new BigDecimal("1900000"));
        assertEquals(target.calculateSfdRate(context, r1, true), new BigDecimal("0"));

        // Tier 1
        when(context.getBestAskPrice(fKey)).thenReturn(new BigDecimal("2200000"));
        assertEquals(target.calculateSfdRate(context, r1, true), new BigDecimal("0.005050000000"));

        // Tier 1+
        when(context.getBestAskPrice(fKey)).thenReturn(new BigDecimal("2240000"));
        assertEquals(target.calculateSfdRate(context, r1, true), new BigDecimal("0.007070000000"));

        // Tier 2
        when(context.getBestAskPrice(fKey)).thenReturn(new BigDecimal("2300000"));
        assertEquals(target.calculateSfdRate(context, r1, true), new BigDecimal("0.010100000000"));

        // Tier 2+
        when(context.getBestAskPrice(fKey)).thenReturn(new BigDecimal("2360000"));
        assertEquals(target.calculateSfdRate(context, r1, true), new BigDecimal("0.022220000000"));

        // Tier 3
        when(context.getBestAskPrice(fKey)).thenReturn(new BigDecimal("2400000"));
        assertEquals(target.calculateSfdRate(context, r1, true), new BigDecimal("0.030300"));

        // Tier 3+
        when(context.getBestAskPrice(fKey)).thenReturn(new BigDecimal("2440000"));
        assertEquals(target.calculateSfdRate(context, r1, true), new BigDecimal("0.030300"));

        // Not FX
        assertEquals(target.calculateSfdRate(context, r2, true), ZERO);

        // Null Price
        when(context.getBestAskPrice(fKey)).thenReturn(null);
        assertEquals(target.calculateSfdRate(context, r1, true), ZERO);

        //
        // Sell
        //

        // Zero SFD
        when(context.getBestBidPrice(fKey)).thenReturn(new BigDecimal("2100000"));
        assertEquals(target.calculateSfdRate(context, r1, false), new BigDecimal("0"));

        // Tier 1
        when(context.getBestBidPrice(fKey)).thenReturn(new BigDecimal("1800000"));
        assertEquals(target.calculateSfdRate(context, r1, false), new BigDecimal("0.005050000000"));

        // Tier 1+
        when(context.getBestBidPrice(fKey)).thenReturn(new BigDecimal("1740000"));
        assertEquals(target.calculateSfdRate(context, r1, false), new BigDecimal("0.008080000000"));

        // Tier 2
        when(context.getBestBidPrice(fKey)).thenReturn(new BigDecimal("1700000"));
        assertEquals(target.calculateSfdRate(context, r1, false), new BigDecimal("0.010100000000"));

        // Tier 2+
        when(context.getBestBidPrice(fKey)).thenReturn(new BigDecimal("1660000"));
        assertEquals(target.calculateSfdRate(context, r1, false), new BigDecimal("0.018180000000"));

        // Tier 3
        when(context.getBestBidPrice(fKey)).thenReturn(new BigDecimal("1600000"));
        assertEquals(target.calculateSfdRate(context, r1, false), new BigDecimal("0.030300"));

        // Tier 3
        when(context.getBestBidPrice(fKey)).thenReturn(new BigDecimal("1540000"));
        assertEquals(target.calculateSfdRate(context, r1, false), new BigDecimal("0.030300"));

        // Not FX
        assertEquals(target.calculateSfdRate(context, r2, false), ZERO);

        // Null Price
        when(context.getBestBidPrice(fKey)).thenReturn(null);
        assertEquals(target.calculateSfdRate(context, r1, false), ZERO);

    }

    @Test
    public void testCalculateDualSfdRate() {

        Request request = Request.builder().build();
        doReturn(new BigDecimal("0.10")).when(target).calculateSfdRate(context, request, true);

        configurations.put(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerAdviser.sfd.inv",
                new BigDecimal("0.50")
        );

        doReturn(new BigDecimal("0.24")).when(target).calculateSfdRate(context, request, false);
        assertEquals(target.calculateDualSfdRate(context, request, true), new BigDecimal("0.1200"));
        assertEquals(target.calculateDualSfdRate(context, request, false), new BigDecimal("0.24"));

        doReturn(new BigDecimal("0.12")).when(target).calculateSfdRate(context, request, false);
        assertEquals(target.calculateDualSfdRate(context, request, true), new BigDecimal("0.10"));
        assertEquals(target.calculateDualSfdRate(context, request, false), new BigDecimal("0.12"));

        doReturn(new BigDecimal("0.08")).when(target).calculateSfdRate(context, request, false);
        assertEquals(target.calculateDualSfdRate(context, request, true), new BigDecimal("0.10"));
        assertEquals(target.calculateDualSfdRate(context, request, false), new BigDecimal("0.08"));

        doReturn(new BigDecimal("0.04")).when(target).calculateSfdRate(context, request, false);
        assertEquals(target.calculateDualSfdRate(context, request, true), new BigDecimal("0.10"));
        assertEquals(target.calculateDualSfdRate(context, request, false), new BigDecimal("0.0500"));

    }

    @Test
    public void testAdjustBuyBasis() {

        Request request = Request.builder().build();

        BigDecimal swap = new BigDecimal("0.01");

        configurations.put(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerAdviser.swap.buy", swap
        );

        doReturn(new BigDecimal("0.0005")).when(target).calculateSwapRate(context, request, swap);
        doReturn(new BigDecimal("0.0001")).when(target).calculateDualSfdRate(context, request, true);

        BigDecimal result = target.adjustBuyBasis(context, request, new BigDecimal("0.002"));
        assertEquals(result, new BigDecimal("0.0026"));

        assertNull(target.adjustBuyBasis(context, request, null));

    }

    @Test
    public void testAdjustSellBasis() {

        Request request = Request.builder().build();

        BigDecimal swap = new BigDecimal("0.01");

        configurations.put(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerAdviser.swap.sell", swap
        );

        doReturn(new BigDecimal("0.0005")).when(target).calculateSwapRate(context, request, swap);
        doReturn(new BigDecimal("0.0001")).when(target).calculateDualSfdRate(context, request, false);

        BigDecimal result = target.adjustSellBasis(context, request, new BigDecimal("0.002"));
        assertEquals(result, new BigDecimal("0.0026"));

        assertNull(target.adjustSellBasis(context, request, null));

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
        BigDecimal swap = new BigDecimal("0.01");

        configurations.put(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerAdviser.swap.buy", swap
        );
        doReturn(key).when(target).getUnderlyingKey(request);
        doReturn(new BigDecimal("0.0004")).when(target).calculateSwapRate(context, request, swap);
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
        BigDecimal swap = new BigDecimal("0.01");

        configurations.put(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerAdviser.swap.sell", swap
        );
        doReturn(key).when(target).getUnderlyingKey(request);
        doReturn(new BigDecimal("0.0004")).when(target).calculateSwapRate(context, request, swap);
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

}
