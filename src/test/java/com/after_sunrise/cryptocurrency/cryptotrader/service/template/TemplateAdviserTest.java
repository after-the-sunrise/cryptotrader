package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import org.mockito.invocation.InvocationOnMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.BiFunction;

import static java.math.BigDecimal.*;
import static java.time.Instant.now;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class TemplateAdviserTest {

    private TemplateAdviser target;

    private Context context;

    private Request.RequestBuilder rBuilder;

    private Estimation.EstimationBuilder eBuilder;

    @BeforeMethod
    public void setUp() throws Exception {

        context = mock(Context.class);

        BiFunction<InvocationOnMock, BigDecimal, BigDecimal> f = (i, unit) -> {

            BigDecimal value = i.getArgumentAt(1, BigDecimal.class);

            RoundingMode mode = i.getArgumentAt(2, RoundingMode.class);

            if (value == null || mode == null) {
                return null;
            }

            BigDecimal units = value.divide(unit, INTEGER_ZERO, mode);

            return units.multiply(unit);

        };
        when(context.roundTickSize(any(), any(), any())).thenAnswer(i -> f.apply(i, new BigDecimal("0.0025")));
        when(context.roundLotSize(any(), any(), any())).thenAnswer(i -> f.apply(i, new BigDecimal("0.25")));

        rBuilder = Request.builder().site("s").instrument("i").targetTime(now())
                .tradingExposure(new BigDecimal("0.10"))
                .tradingSplit(new BigDecimal("2")).tradingSpread(new BigDecimal("0.0060"));

        eBuilder = Estimation.builder().price(new BigDecimal("12345.6789")).confidence(new BigDecimal("0.5"));

        target = spy(new TemplateAdviser("test"));

    }

    @Test
    public void testGet() throws Exception {
        assertEquals(target.get(), "test");
    }

    @Test
    public void testAdvise() throws Exception {

        Request request = rBuilder.build();
        Estimation estimation = eBuilder.build();

        doReturn(valueOf(1)).when(target).calculateBuyLimitPrice(context, request, estimation);
        doReturn(valueOf(2)).when(target).calculateBuyLimitSize(context, request, valueOf(1));
        doReturn(valueOf(3)).when(target).calculateSellLimitPrice(context, request, estimation);
        doReturn(valueOf(4)).when(target).calculateSellLimitSize(context, request);

        Advice result = target.advise(context, request, estimation);
        assertEquals(result.getBuyLimitPrice(), valueOf(1));
        assertEquals(result.getBuyLimitSize(), valueOf(2));
        assertEquals(result.getSellLimitPrice(), valueOf(3));
        assertEquals(result.getSellLimitSize(), valueOf(4));

        result = target.advise(context, null, estimation);
        assertNull(result.getBuyLimitPrice());
        assertNull(result.getBuyLimitSize());
        assertNull(result.getSellLimitPrice());
        assertNull(result.getSellLimitSize());

        result = target.advise(context, request, null);
        assertNull(result.getBuyLimitPrice());
        assertNull(result.getBuyLimitSize());
        assertNull(result.getSellLimitPrice());
        assertNull(result.getSellLimitSize());

    }

    @Test
    public void testCalculatePositionRatio() {

        Request request = rBuilder.build();
        Key key = Key.from(request);

        // Net-Long
        // Fund = 9,800 : Structure = 5 * 2345 = 11,725
        // (11725 - 9800) / (11725 + 9800) = 0.0894308943089...
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("9800"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("5"));
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        assertEquals(target.calculatePositionRatio(context, request), new BigDecimal("0.089430894309"));

        // Long-only
        // Fund = 0 : Structure = 5 * 2345 = 11,725
        // (11725 - 0) / (11725 + 0) = 1
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("0"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("5"));
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        assertEquals(target.calculatePositionRatio(context, request), new BigDecimal("1.000000000000"));

        // Net-Short
        // Fund = 12,345 : Structure = 5 * 2,345 = 11,725
        // (11,725 - 12,345) / (11,725 + 12,345) = -0.025758205234732...
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("12345"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("5"));
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        assertEquals(target.calculatePositionRatio(context, request), new BigDecimal("-0.025758205235"));

        // Short-Only
        // Fund = 12,345 : Structure = 0
        // (0 - 12,345) / (0 + 12,345) = -1
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("12345"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("0"));
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        assertEquals(target.calculatePositionRatio(context, request), new BigDecimal("-1.000000000000"));

    }

    @Test
    public void testCalculateBuyLimitPrice() throws Exception {

        Request request = rBuilder.build();
        Key key = Key.from(request);
        Estimation estimation = eBuilder.build();
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("12349.8765"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("3"));
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("50000"));
        when(context.getCommissionRate(key)).thenReturn(new BigDecimal("0.0020"));

        // Estimated : price 12345.6789 | confidence 0.5
        // Weighed : 12347.7777
        // Spread : 60 * 1.0000 = 60 bps
        // Spread : Average * (1 - (60 + 20 bps)) = 12248.99548
        // Rounded : 12248.9950
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("20000"));
        assertEquals(target.calculateBuyLimitPrice(context, request, estimation), new BigDecimal("12248.9950"));

        // ...
        // Spread : 60 * 1.29885609511276..  = 77.9313657.. bps
        // Spread : Average * (1 - (77.93 + 20 bps)) = 12226.85423
        // Rounded : 12226.8525
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("20000"));
        assertEquals(target.calculateBuyLimitPrice(context, request, estimation), new BigDecimal("12226.8525"));

        // Cross protection : Ask 10000 * (1 - (77.93 + 20 bps)) = 9902.068634
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("10000"));
        assertEquals(target.calculateBuyLimitPrice(context, request, estimation), new BigDecimal("9902.0675"));

        // No Mid
        when(context.getMidPrice(key)).thenReturn(null);
        when(context.getBestAskPrice(key)).thenReturn(ONE);
        when(context.getCommissionRate(key)).thenReturn(ZERO);
        assertNull(target.calculateBuyLimitPrice(context, request, estimation));

        // No Ask
        when(context.getMidPrice(key)).thenReturn(ONE);
        when(context.getBestAskPrice(key)).thenReturn(null);
        when(context.getCommissionRate(key)).thenReturn(ZERO);
        assertNull(target.calculateBuyLimitPrice(context, request, estimation));

        // No Commission
        when(context.getMidPrice(key)).thenReturn(ONE);
        when(context.getBestAskPrice(key)).thenReturn(ONE);
        when(context.getCommissionRate(key)).thenReturn(null);
        assertNull(target.calculateBuyLimitPrice(context, request, estimation));

    }

    @Test
    public void testCalculateSellLimitPrice() throws Exception {

        Request request = rBuilder.build();
        Key key = Key.from(request);
        Estimation estimation = eBuilder.build();
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("12349.8765"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("3"));
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("20000"));
        when(context.getCommissionRate(key)).thenReturn(new BigDecimal("0.0020"));

        // Estimated : price 12345.6789 | confidence 0.5
        // Weighed : 12347.7777
        // Spread : 60 * 1.0000 = 60 bps
        // Target : Average * (1 - (60 + 20 bps)) = 12446.55992
        // Rounded : 12446.5600
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("10000"));
        assertEquals(target.calculateSellLimitPrice(context, request, estimation), new BigDecimal("12446.5600"));

        // ...
        // Spread : 60 * 1.14876996690721..  = 68.9261980.. bps
        // Target : Average * (1 - (68.92 + 20 bps)) = 12457.58179
        // Rounded : 12457.5825
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("50000"));
        assertEquals(target.calculateSellLimitPrice(context, request, estimation), new BigDecimal("12457.5825"));

        // Cross protection : Bid 20000 * (1 - (68.92 + 20 bps)) = 20177.8524
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("20000"));
        assertEquals(target.calculateSellLimitPrice(context, request, estimation), new BigDecimal("20177.8525"));

        // No Mid
        when(context.getMidPrice(key)).thenReturn(null);
        when(context.getBestBidPrice(key)).thenReturn(ONE);
        when(context.getCommissionRate(key)).thenReturn(ZERO);
        assertNull(target.calculateSellLimitPrice(context, request, estimation));

        // No Bid
        when(context.getMidPrice(key)).thenReturn(ONE);
        when(context.getBestBidPrice(key)).thenReturn(null);
        when(context.getCommissionRate(key)).thenReturn(ZERO);
        assertNull(target.calculateSellLimitPrice(context, request, estimation));

        // No Commission
        when(context.getMidPrice(key)).thenReturn(ONE);
        when(context.getBestBidPrice(key)).thenReturn(ONE);
        when(context.getCommissionRate(key)).thenReturn(null);
        assertNull(target.calculateSellLimitPrice(context, request, estimation));

    }

    @Test
    public void testCalculateBuyLimitSize() throws Exception {

        Request request = rBuilder.build();
        Key key = Key.from(request);
        BigDecimal price = new BigDecimal("123.4567");
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("9000"));

        // Exposed = Fund * Exposure = 900
        // Fund / Price = 7.290005322...
        // Rounded = 7.25
        assertEquals(target.calculateBuyLimitSize(context, request, price), new BigDecimal("7.25"));

        // No price
        assertEquals(target.calculateBuyLimitSize(context, request, null), ZERO);
        assertEquals(target.calculateBuyLimitSize(context, request, ZERO), ZERO);

        // No Fund
        when(context.getFundingPosition(key)).thenReturn(ZERO);
        assertEquals(target.calculateBuyLimitSize(context, request, price), ZERO);

        // Null Fund
        when(context.getFundingPosition(key)).thenReturn(null);
        assertEquals(target.calculateBuyLimitSize(context, request, price), ZERO);

    }

    @Test
    public void testCalculateSellLimitSize() throws Exception {

        Request request = rBuilder.build();
        Key key = Key.from(request);

        // Exposure  = Instrument * Exposure = 12.3456789
        // Rounded = 12.25
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("123.456789"));
        assertEquals(target.calculateSellLimitSize(context, request), new BigDecimal("12.25"));

        // No position
        when(context.getInstrumentPosition(key)).thenReturn(ZERO);
        assertEquals(target.calculateSellLimitSize(context, request), ZERO);

        // Null position
        when(context.getInstrumentPosition(key)).thenReturn(null);
        assertEquals(target.calculateSellLimitSize(context, request), ZERO);

    }

}
