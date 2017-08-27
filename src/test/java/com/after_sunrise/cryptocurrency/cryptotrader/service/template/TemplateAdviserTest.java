package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import org.mockito.invocation.InvocationOnMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.BiFunction;

import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ZERO;
import static java.math.BigDecimal.valueOf;
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
                .tradingSplit(new BigDecimal("2"))
                .tradingSpread(new BigDecimal("0.0060"))
                .fundingOffset(new BigDecimal("-0.50"));

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
        doReturn(valueOf(9)).when(target).calculateWeighedPrice(context, request, estimation);
        doReturn(valueOf(8)).when(target).calculateBasis(context, request);

        doReturn(valueOf(1)).when(target).calculateBuyLimitPrice(context, request, valueOf(9), valueOf(8));
        doReturn(valueOf(2)).when(target).calculateBuyLimitSize(context, request, valueOf(1));
        doReturn(valueOf(3)).when(target).calculateSellLimitPrice(context, request, valueOf(9), valueOf(8));
        doReturn(valueOf(4)).when(target).calculateSellLimitSize(context, request, valueOf(3));

        Advice result = target.advise(context, request, estimation);
        assertEquals(result.getBuyLimitPrice(), valueOf(1));
        assertEquals(result.getBuyLimitSize(), valueOf(2));
        assertEquals(result.getSellLimitPrice(), valueOf(3));
        assertEquals(result.getSellLimitSize(), valueOf(4));

        // Invalid Request
        result = target.advise(context, Request.builder().build(), estimation);
        assertEquals(result.getBuyLimitPrice(), null);
        assertEquals(result.getBuyLimitSize(), ZERO);
        assertEquals(result.getSellLimitPrice(), null);
        assertEquals(result.getSellLimitSize(), ZERO);

        // Invalid Estimation
        result = target.advise(context, request, Estimation.builder().build());
        assertEquals(result.getBuyLimitPrice(), null);
        assertEquals(result.getBuyLimitSize(), ZERO);
        assertEquals(result.getSellLimitPrice(), null);
        assertEquals(result.getSellLimitSize(), ZERO);

    }

    @Test
    public void testCalculateBasis() {

        Request request = Request.builder().tradingSpread(new BigDecimal("0.0060")).build();
        when(context.getCommissionRate(Key.from(request))).thenReturn(new BigDecimal("0.0020"));
        assertEquals(target.calculateBasis(context, request), new BigDecimal("0.0080"));

        // Null spread
        request = Request.builder().tradingSpread(null).build();
        when(context.getCommissionRate(Key.from(request))).thenReturn(new BigDecimal("0.0020"));
        assertNull(target.calculateBasis(context, request));

        // Null commission
        request = Request.builder().tradingSpread(new BigDecimal("0.0060")).build();
        when(context.getCommissionRate(Key.from(request))).thenReturn(null);
        assertNull(target.calculateBasis(context, request));

    }

    @Test
    public void testCalculatePositionRatio_Cash() {

        Request request = rBuilder.build();
        Key key = Key.from(request);
        when(context.isMarginable(key)).thenReturn(null);

        // Net-Long
        // Fund = 9,800 : Structure = 5 * 2345 = 11,725
        // (11725 - 9800) / (11725 + 9800) = 0.0894308943089...
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("19600"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("5"));
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        assertEquals(target.calculatePositionRatio(context, request), new BigDecimal("0.1788617886"));

        // Long-only
        // Fund = 0 : Structure = 5 * 2345 = 11,725
        // (11725 - 0) / (11725 + 0) = 1
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("0"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("5"));
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        assertEquals(target.calculatePositionRatio(context, request), new BigDecimal("2.0000000000"));

        // Net-Short
        // Fund = 12,345 : Structure = 5 * 2,345 = 11,725
        // (11,725 - 12,345) / (11,725 + 12,345) = -0.025758205234732...
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("24690"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("5"));
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        assertEquals(target.calculatePositionRatio(context, request), new BigDecimal("-0.0515164105"));

        // Short-Only
        // Fund = 12,345 : Structure = 0
        // (0 - 12,345) / (0 + 12,345) = -1
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("24690"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("0"));
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        assertEquals(target.calculatePositionRatio(context, request), new BigDecimal("-2.0000000000"));

        // Zero Asset
        when(context.getFundingPosition(key)).thenReturn(ZERO);
        when(context.getInstrumentPosition(key)).thenReturn(ZERO);
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        assertEquals(target.calculatePositionRatio(context, request), ZERO);

        // Null Funding
        when(context.getFundingPosition(key)).thenReturn(null);
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("0"));
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        assertNull(target.calculatePositionRatio(context, request));

        // Null Structure
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("24690"));
        when(context.getInstrumentPosition(key)).thenReturn(null);
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        assertNull(target.calculatePositionRatio(context, request));

        // Null Mid Price
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("24690"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("0"));
        when(context.getMidPrice(key)).thenReturn(null);
        assertNull(target.calculatePositionRatio(context, request));

    }

    @Test
    public void testCalculatePositionRatio_Margin() {

        Request request = rBuilder.build();
        Key key = Key.from(request);
        when(context.isMarginable(key)).thenReturn(true);

        // Long (2345 * 3 / 9800 = 0.71785714285714..)
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("3"));
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("19600"));
        assertEquals(target.calculatePositionRatio(context, request), new BigDecimal("0.7178571429"));

        // Leveraged Long (2345 * 5 / 9800 = 1.19642857142857..)
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("5"));
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("19600"));
        assertEquals(target.calculatePositionRatio(context, request), new BigDecimal("1.1964285714"));

        // Short (2345 * -3 / 9800 = -0.71785714285714..)
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("-3"));
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("19600"));
        assertEquals(target.calculatePositionRatio(context, request), new BigDecimal("-0.7178571429"));

        // Leveraged Short (2345 * -5 / 9800 = -1.19642857142857..)
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("-5"));
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("19600"));
        assertEquals(target.calculatePositionRatio(context, request), new BigDecimal("-1.1964285714"));

        // Flat (2345 * 0 / 9800 = 0)
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        when(context.getInstrumentPosition(key)).thenReturn(ZERO);
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("19600"));
        assertEquals(target.calculatePositionRatio(context, request).signum(), 0);

        // Zero Funding
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        when(context.getInstrumentPosition(key)).thenReturn(ZERO);
        when(context.getFundingPosition(key)).thenReturn(ZERO);
        assertEquals(target.calculatePositionRatio(context, request).signum(), 0);

        // Null Funding
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        when(context.getInstrumentPosition(key)).thenReturn(ZERO);
        when(context.getFundingPosition(key)).thenReturn(null);
        assertNull(target.calculatePositionRatio(context, request));

        // Null Structure
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        when(context.getInstrumentPosition(key)).thenReturn(null);
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("19600"));
        assertNull(target.calculatePositionRatio(context, request));

        // Null Mid Price
        when(context.getMidPrice(key)).thenReturn(null);
        when(context.getInstrumentPosition(key)).thenReturn(ZERO);
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("19600"));
        assertNull(target.calculatePositionRatio(context, request));

    }

    @Test
    public void testCalculateBuyLimitPrice() throws Exception {

        Request request = rBuilder.build();
        Key key = Key.from(request);

        BigDecimal weighed = new BigDecimal("12347.7777");
        BigDecimal basis = new BigDecimal("0.0080");

        // [Net Long]
        // Weighed : 12347.7777
        // Basis : 80 bps * (1 + 0.2) = 96 bps
        // Target : 12347.7777 * (1 - 96 bps) = 12229.23903408
        // Cross : min(12229.23903408, 20000)
        // Rounded : 12229.2375
        doReturn(new BigDecimal("0.2")).when(target).calculatePositionRatio(context, request);
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("20000"));
        BigDecimal result = target.calculateBuyLimitPrice(context, request, weighed, basis);
        assertEquals(result, new BigDecimal("12229.2375"));

        // [Net Short]
        // Weighed : 12347.7777
        // Basis : 80 bps * (1) = 80 bps
        // Target : 12347.7777 * (1 - 80 bps) = 12248.9954784
        // Cross : min(12248.9954784, 20000)
        // Rounded : 12248.9950
        doReturn(new BigDecimal("-0.2")).when(target).calculatePositionRatio(context, request);
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("20000"));
        result = target.calculateBuyLimitPrice(context, request, weighed, basis);
        assertEquals(result, new BigDecimal("12248.9950"));

        // Cross Protected
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("10000"));
        result = target.calculateBuyLimitPrice(context, request, weighed, basis);
        assertEquals(result, new BigDecimal("9999.9975"));

        // Invalid Input
        assertNull(target.calculateBuyLimitPrice(context, request, null, basis));
        assertNull(target.calculateBuyLimitPrice(context, request, weighed, null));

        // Null Ratio
        doReturn(null).when(target).calculatePositionRatio(context, request);
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("20000"));
        assertNull(target.calculateBuyLimitPrice(context, request, weighed, basis));

        // Null Ask
        doReturn(new BigDecimal("0.2")).when(target).calculatePositionRatio(context, request);
        when(context.getBestAskPrice(key)).thenReturn(null);
        assertNull(target.calculateBuyLimitPrice(context, request, weighed, basis));

    }

    @Test
    public void testCalculateSellLimitPrice() throws Exception {

        Request request = rBuilder.build();
        Key key = Key.from(request);

        BigDecimal weighed = new BigDecimal("12347.7777");
        BigDecimal basis = new BigDecimal("0.0080");

        // [Net Long]
        // Weighed : 12347.7777
        // Basis : 80 bps * (1) = 80 bps
        // Target : 12347.7777 * (1 + 80 bps) = 12446.5599216
        // Cross : max(12446.5599216, 10000)
        // Rounded : 12446.5600
        doReturn(new BigDecimal("0.2")).when(target).calculatePositionRatio(context, request);
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("10000"));
        BigDecimal result = target.calculateSellLimitPrice(context, request, weighed, basis);
        assertEquals(result, new BigDecimal("12446.5600"));

        // [Net Short]
        // Weighed : 12347.7777
        // Basis : 80 bps * (1 + 0.2) = 96 bps
        // Target : 12347.7777 * (1 + 96 bps) = 12466.31636592
        // Cross : max(12466.31636592, 10000)
        // Rounded : 12466.3175
        doReturn(new BigDecimal("-0.2")).when(target).calculatePositionRatio(context, request);
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("10000"));
        result = target.calculateSellLimitPrice(context, request, weighed, basis);
        assertEquals(result, new BigDecimal("12466.3175"));

        // Cross Protected
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("20000"));
        result = target.calculateSellLimitPrice(context, request, weighed, basis);
        assertEquals(result, new BigDecimal("20000.0025"));

        // Invalid Input
        assertNull(target.calculateSellLimitPrice(context, request, null, basis));
        assertNull(target.calculateSellLimitPrice(context, request, weighed, null));

        // Null Ratio
        doReturn(null).when(target).calculatePositionRatio(context, request);
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("20000"));
        assertNull(target.calculateSellLimitPrice(context, request, weighed, basis));

        // Null Ask
        doReturn(new BigDecimal("0.2")).when(target).calculatePositionRatio(context, request);
        when(context.getBestBidPrice(key)).thenReturn(null);
        assertNull(target.calculateSellLimitPrice(context, request, weighed, basis));

    }

    @Test
    public void testCalculateFundingLimitSize() {

        Request request = rBuilder.tradingExposure(new BigDecimal("0.10")).build();
        Key key = Key.from(request);
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("18000"));
        BigDecimal price = new BigDecimal("123.4567");

        // Exposed = Fund * Exposure = 900
        // Fund / Price = 900 / 123.4567 = 7.290005322...
        // Rounded = 7.25
        assertEquals(target.calculateFundingLimitSize(context, request, price), new BigDecimal("7.25"));

        // Invalid price
        assertEquals(target.calculateFundingLimitSize(context, request, null), ZERO);
        assertEquals(target.calculateFundingLimitSize(context, request, ZERO), ZERO);

        // Invalid Fund
        when(context.getFundingPosition(key)).thenReturn(null);
        assertEquals(target.calculateFundingLimitSize(context, request, price), ZERO);

        // Rounding Failure
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("18000"));
        when(context.roundLotSize(any(), any(), any())).thenReturn(null);
        assertEquals(target.calculateFundingLimitSize(context, request, price), ZERO);

    }

    @Test
    public void testCalculateBuyLimitSize_Cash() throws Exception {

        Request request = rBuilder.build();
        Key key = Key.from(request);
        when(context.isMarginable(key)).thenReturn(null);
        BigDecimal price = new BigDecimal("123.4567");
        doReturn(new BigDecimal("7.25")).when(target).calculateFundingLimitSize(context, request, price);

        // With Fund
        assertEquals(target.calculateBuyLimitSize(context, request, price), new BigDecimal("7.25"));

        // Null Fund
        doReturn(null).when(target).calculateFundingLimitSize(context, request, price);
        assertEquals(target.calculateBuyLimitSize(context, request, price), null);

    }

    @Test
    public void testCalculateBuyLimitSize_Margin() throws Exception {

        Request request = rBuilder.build();
        Key key = Key.from(request);
        when(context.isMarginable(key)).thenReturn(true);
        BigDecimal price = new BigDecimal("123.4567");

        // Net-Long
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("4"));
        doReturn(new BigDecimal("7.25")).when(target).calculateFundingLimitSize(context, request, price);
        assertEquals(target.calculateBuyLimitSize(context, request, price), new BigDecimal("7.25"));

        // Net-Short
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("-4"));
        doReturn(new BigDecimal("7.25")).when(target).calculateFundingLimitSize(context, request, price);
        assertEquals(target.calculateBuyLimitSize(context, request, price), new BigDecimal("11.25"));

        // Null fund
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("-4"));
        doReturn(null).when(target).calculateFundingLimitSize(context, request, price);
        assertEquals(target.calculateBuyLimitSize(context, request, price), ZERO);

        // Null structure
        when(context.getInstrumentPosition(key)).thenReturn(null);
        doReturn(new BigDecimal("7.25")).when(target).calculateFundingLimitSize(context, request, price);
        assertEquals(target.calculateBuyLimitSize(context, request, price), ZERO);

    }

    @Test
    public void testCalculateSellLimitSize_Cash() throws Exception {

        Request request = rBuilder.build();
        Key key = Key.from(request);
        when(context.isMarginable(key)).thenReturn(null);

        // Exposure  = Instrument * Exposure = 12.3456789
        // Rounded = 12.25
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("123.456789"));
        assertEquals(target.calculateSellLimitSize(context, request, null), new BigDecimal("12.25"));

        // No position
        when(context.getInstrumentPosition(key)).thenReturn(ZERO);
        assertEquals(target.calculateSellLimitSize(context, request, null).signum(), 0);

        // Null position
        when(context.getInstrumentPosition(key)).thenReturn(null);
        assertEquals(target.calculateSellLimitSize(context, request, null).signum(), 0);

    }

    @Test
    public void testCalculateSellLimitSize_Margin() throws Exception {

        Request request = rBuilder.build();
        Key key = Key.from(request);
        when(context.isMarginable(key)).thenReturn(TRUE);

        // Exposed = Fund * Exposure = 900
        // Fund / Price = 7.290005322...
        // Rounded = 7.25
        BigDecimal price = new BigDecimal("123.4567");
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("18000"));

        // Net Position = 7.25 + (+4.75) = 12
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("+4.75"));
        assertEquals(target.calculateSellLimitSize(context, request, price), new BigDecimal("12.00"));

        // Net Position = (ignore short)
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("-4.75"));
        assertEquals(target.calculateSellLimitSize(context, request, price), new BigDecimal("7.25"));

        // Net Position = 7.25 + (-9.75) = -2.5 -> 0
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("-9.75"));
        assertEquals(target.calculateSellLimitSize(context, request, price), new BigDecimal("7.25"));

        // Null Position = 7.25 + null = null
        when(context.getInstrumentPosition(key)).thenReturn(null);
        assertEquals(target.calculateSellLimitSize(context, request, price), ZERO);

    }

}
