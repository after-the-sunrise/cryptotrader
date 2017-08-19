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
                .tradingSplit(new BigDecimal("2")).tradingSpread(new BigDecimal("0.0080"));

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
    public void testCalculateBuyLimitPrice() throws Exception {

        Request request = rBuilder.build();
        Estimation estimation = eBuilder.build();
        when(context.getMidPrice(Key.from(request))).thenReturn(new BigDecimal("12349.8765"));

        // Estimated : price 12345.6789 | confidence 0.5
        // Weighed : 12347.7777
        // Spread : Average * (1 - 80 bps) = 12248.99548
        // Rounded : 12248.9950
        when(context.getBestAskPrice(Key.from(request))).thenReturn(new BigDecimal("20000"));
        assertEquals(target.calculateBuyLimitPrice(context, request, estimation), new BigDecimal("12248.9950"));

        // Cross protection (10000 -> 9920)
        when(context.getBestAskPrice(Key.from(request))).thenReturn(new BigDecimal("10000"));
        assertEquals(target.calculateBuyLimitPrice(context, request, estimation), new BigDecimal("9919.9975"));

        // No Mid
        when(context.getMidPrice(Key.from(request))).thenReturn(null);
        when(context.getBestAskPrice(Key.from(request))).thenReturn(ONE);
        assertNull(target.calculateBuyLimitPrice(context, request, estimation));

        // No Ask
        when(context.getMidPrice(Key.from(request))).thenReturn(ONE);
        when(context.getBestAskPrice(Key.from(request))).thenReturn(null);
        assertNull(target.calculateBuyLimitPrice(context, request, estimation));

    }

    @Test
    public void testCalculateSellLimitPrice() throws Exception {

        Request request = rBuilder.build();
        Estimation estimation = eBuilder.build();
        when(context.getMidPrice(Key.from(request))).thenReturn(new BigDecimal("12349.8765"));

        // Estimated : price 12345.6789 | confidence 0.5
        // Weighed : 12347.7777
        // Spread : Average * (1 - 80 bps) = 12446.55992
        // Rounded : 12446.5600
        when(context.getBestBidPrice(Key.from(request))).thenReturn(new BigDecimal("10000"));
        assertEquals(target.calculateSellLimitPrice(context, request, estimation), new BigDecimal("12446.5600"));

        // Cross protection (20000 -> 20160)
        when(context.getBestBidPrice(Key.from(request))).thenReturn(new BigDecimal("20000"));
        assertEquals(target.calculateSellLimitPrice(context, request, estimation), new BigDecimal("20160.0025"));

        // No Mid
        when(context.getMidPrice(Key.from(request))).thenReturn(null);
        when(context.getBestBidPrice(Key.from(request))).thenReturn(ONE);
        assertNull(target.calculateSellLimitPrice(context, request, estimation));

        // No Bid
        when(context.getMidPrice(Key.from(request))).thenReturn(ONE);
        when(context.getBestBidPrice(Key.from(request))).thenReturn(null);
        assertNull(target.calculateSellLimitPrice(context, request, estimation));

    }

    @Test
    public void testCalculateBuyLimitSize() throws Exception {

        Request request = rBuilder.build();
        BigDecimal price = new BigDecimal("123.4567");
        when(context.getFundingPosition(Key.from(request))).thenReturn(new BigDecimal("9000"));

        // Exposed = Fund * Exposure = 900
        // Fund / Price = 7.290005322...
        // Rounded = 7.25
        assertEquals(target.calculateBuyLimitSize(context, request, price), new BigDecimal("7.25"));

        // No price
        assertEquals(target.calculateBuyLimitSize(context, request, null), ZERO);
        assertEquals(target.calculateBuyLimitSize(context, request, ZERO), ZERO);

        // No Fund
        when(context.getFundingPosition(Key.from(request))).thenReturn(ZERO);
        assertEquals(target.calculateBuyLimitSize(context, request, price), ZERO);

        // Null Fund
        when(context.getFundingPosition(Key.from(request))).thenReturn(null);
        assertEquals(target.calculateBuyLimitSize(context, request, price), ZERO);

    }

    @Test
    public void testCalculateSellLimitSize() throws Exception {

        Request request = rBuilder.build();

        // Exposure  = Instrument * Exposure = 12.3456789
        // Rounded = 12.25
        when(context.getInstrumentPosition(Key.from(request))).thenReturn(new BigDecimal("123.456789"));
        assertEquals(target.calculateSellLimitSize(context, request), new BigDecimal("12.25"));

        // No position
        when(context.getInstrumentPosition(Key.from(request))).thenReturn(ZERO);
        assertEquals(target.calculateSellLimitSize(context, request), ZERO);

        // Null position
        when(context.getInstrumentPosition(Key.from(request))).thenReturn(null);
        assertEquals(target.calculateSellLimitSize(context, request), ZERO);

    }

}
