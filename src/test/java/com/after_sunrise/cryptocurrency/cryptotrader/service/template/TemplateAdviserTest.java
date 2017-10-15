package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order.Execution;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import org.mockito.invocation.InvocationOnMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateAdviser.SIGNUM_BUY;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateAdviser.SIGNUM_SELL;
import static java.math.BigDecimal.*;
import static java.time.Instant.now;
import static java.util.Collections.singletonList;
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
        BigDecimal weighed = valueOf(100);
        BigDecimal basis = valueOf(200);
        BigDecimal bBasis = valueOf(201);
        BigDecimal sBasis = valueOf(202);
        BigDecimal bPrice = valueOf(301);
        BigDecimal sPrice = valueOf(302);
        BigDecimal bSize = valueOf(401);
        BigDecimal sSize = valueOf(402);

        Estimation estimation = eBuilder.build();
        doReturn(weighed).when(target).calculateWeighedPrice(context, request, estimation);
        doReturn(basis).when(target).calculateBasis(context, request);
        doReturn(bBasis).when(target).calculateBuyBasis(context, request, basis);
        doReturn(sBasis).when(target).calculateSellBasis(context, request, basis);

        doReturn(bPrice).when(target).calculateBuyLimitPrice(context, request, weighed, bBasis);
        doReturn(bSize).when(target).calculateBuyLimitSize(context, request, bPrice);
        doReturn(sPrice).when(target).calculateSellLimitPrice(context, request, weighed, sBasis);
        doReturn(sSize).when(target).calculateSellLimitSize(context, request, sPrice);

        Advice result = target.advise(context, request, estimation);
        assertEquals(result.getBuyLimitPrice(), bPrice);
        assertEquals(result.getBuyLimitSize(), bSize);
        assertEquals(result.getSellLimitPrice(), sPrice);
        assertEquals(result.getSellLimitSize(), sSize);

        // Invalid Request
        result = target.advise(context, Request.builder().build(), estimation);
        assertEquals(result.getBuyLimitPrice(), null);
        assertEquals(result.getBuyLimitSize().signum(), 0);
        assertEquals(result.getSellLimitPrice(), null);
        assertEquals(result.getSellLimitSize().signum(), 0);

        // Invalid Estimation
        result = target.advise(context, request, Estimation.builder().build());
        assertEquals(result.getBuyLimitPrice(), null);
        assertEquals(result.getBuyLimitSize().signum(), 0);
        assertEquals(result.getSellLimitPrice(), null);
        assertEquals(result.getSellLimitSize().signum(), 0);

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
        Request aversion = rBuilder.tradingAversion(new BigDecimal("1.5")).build();
        Request ignore = rBuilder.tradingAversion(new BigDecimal("0.0")).build();
        Key key = Key.from(request);
        when(context.isMarginable(key)).thenReturn(null);

        // Net-Long
        // Fund = 9,800 : Structure = 5 * 2345 = 11,725
        // (11725 - 9800) / (11725 + 9800) = 0.0894308943089...
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("19600"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("5"));
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        assertEquals(target.calculatePositionRatio(context, request), new BigDecimal("0.1788617886"));

        // Net-Long (aversion)
        // Fund = 9,800 : Structure = 5 * 2345 = 11,725
        // (11725 - 9800) / (11725 + 9800) = 0.0894308943089...
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("19600"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("5"));
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        assertEquals(target.calculatePositionRatio(context, aversion), new BigDecimal("0.2682926829"));

        // Zero aversion
        assertEquals(target.calculatePositionRatio(context, ignore), ZERO);

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

        // Net-Short (aversion)
        // Fund = 12,345 : Structure = 5 * 2,345 = 11,725
        // (11,725 - 12,345) / (11,725 + 12,345) = -0.025758205234732...
        // Aversion = 1.5
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("24690"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("5"));
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        assertEquals(target.calculatePositionRatio(context, aversion), new BigDecimal("-0.0772746158"));

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
        Request aversion = rBuilder.tradingAversion(new BigDecimal("1.5")).build();
        Request ignore = rBuilder.tradingAversion(new BigDecimal("0.0")).build();
        Key key = Key.from(request);
        when(context.isMarginable(key)).thenReturn(true);

        // Long (2 * 2345 * 3  / 9800 = 0.71785714285714..)
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("3"));
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("19600"));
        assertEquals(target.calculatePositionRatio(context, request), new BigDecimal("1.4357142857"));

        // Leveraged Long (2 * 2345 * 5 / 9800 = 1.19642857142857..)
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("5"));
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("19600"));
        assertEquals(target.calculatePositionRatio(context, request), new BigDecimal("2.3928571429"));

        // Aversion Leveraged Long (2 * 2345 * 5 / 9800 = 1.19642857142857..)
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("5"));
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("19600"));
        assertEquals(target.calculatePositionRatio(context, aversion), new BigDecimal("3.5892857144"));

        // Zero aversion
        assertEquals(target.calculatePositionRatio(context, ignore), ZERO);

        // Short (2 * 2345 * -3 / 9800 = -0.71785714285714..)
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("-3"));
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("19600"));
        assertEquals(target.calculatePositionRatio(context, request), new BigDecimal("-1.4357142857"));

        // Leveraged Short (2 * 2345 * -5 / 9800 = -1.19642857142857..)
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("-5"));
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("19600"));
        assertEquals(target.calculatePositionRatio(context, request), new BigDecimal("-2.3928571429"));

        // Aversion Leveraged Short (2 * 2345 * -5 / 9800 = -1.19642857142857..)
        when(context.getMidPrice(key)).thenReturn(new BigDecimal("2345"));
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("-5"));
        when(context.getFundingPosition(key)).thenReturn(new BigDecimal("19600"));
        assertEquals(target.calculatePositionRatio(context, aversion), new BigDecimal("-3.5892857144"));

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
    public void testCalculateRecentPrice() {

        // Include 51 ~ 150
        Instant now = Instant.ofEpochMilli(150);
        Duration duration = Duration.ofMillis(100);
        Request request = rBuilder.currentTime(now).tradingDuration(duration).build();
        doReturn(null).when(target).calculateBasis(context, request);

        // Null Executions
        when(context.listExecutions(Key.from(request))).thenReturn(null);
        assertNull(target.calculateRecentPrice(context, request, SIGNUM_BUY));
        assertNull(target.calculateRecentPrice(context, request, SIGNUM_SELL));

        // Empty Executions
        List<Execution> values = new ArrayList<>();
        when(context.listExecutions(Key.from(request))).thenReturn(values);
        assertNull(target.calculateRecentPrice(context, request, SIGNUM_BUY));
        assertNull(target.calculateRecentPrice(context, request, SIGNUM_SELL));

        // New Long : 10@123
        Execution exec = mock(Execution.class);
        when(exec.getTime()).thenReturn(Instant.ofEpochMilli(55));
        when(exec.getPrice()).thenReturn(new BigDecimal("123"));
        when(exec.getSize()).thenReturn(new BigDecimal("10"));
        values.add(exec);
        assertEquals(target.calculateRecentPrice(context, request, SIGNUM_BUY), new BigDecimal("123"));
        assertNull(target.calculateRecentPrice(context, request, SIGNUM_SELL));

        // Increased Long : 10@123 + 10@124
        exec = mock(Execution.class);
        when(exec.getTime()).thenReturn(Instant.ofEpochMilli(60));
        when(exec.getPrice()).thenReturn(new BigDecimal("124"));
        when(exec.getSize()).thenReturn(new BigDecimal("10"));
        values.add(exec);
        assertEquals(target.calculateRecentPrice(context, request, SIGNUM_BUY), new BigDecimal("124"));
        assertNull(target.calculateRecentPrice(context, request, SIGNUM_SELL));

        // Increased Long : 10@123 + 10@124 + 20@121
        exec = mock(Execution.class);
        when(exec.getTime()).thenReturn(Instant.ofEpochMilli(60));
        when(exec.getPrice()).thenReturn(new BigDecimal("121"));
        when(exec.getSize()).thenReturn(new BigDecimal("20"));
        values.add(exec);
        assertEquals(target.calculateRecentPrice(context, request, SIGNUM_BUY), new BigDecimal("124"));
        assertNull(target.calculateRecentPrice(context, request, SIGNUM_SELL));

        // Flipped Short : (10@123 + 10@124 + 20@121) + -60@126 = -20@126
        exec = mock(Execution.class);
        when(exec.getTime()).thenReturn(Instant.ofEpochMilli(65));
        when(exec.getPrice()).thenReturn(new BigDecimal("126"));
        when(exec.getSize()).thenReturn(new BigDecimal("-60"));
        values.add(exec);
        assertNull(target.calculateRecentPrice(context, request, SIGNUM_BUY));
        assertEquals(target.calculateRecentPrice(context, request, SIGNUM_SELL), new BigDecimal("126"));

        // Increased Short : -20@126 + -20@127
        exec = mock(Execution.class);
        when(exec.getTime()).thenReturn(Instant.ofEpochMilli(70));
        when(exec.getPrice()).thenReturn(new BigDecimal("127"));
        when(exec.getSize()).thenReturn(new BigDecimal("-20"));
        values.add(exec);
        assertNull(target.calculateRecentPrice(context, request, SIGNUM_BUY));
        assertEquals(target.calculateRecentPrice(context, request, SIGNUM_SELL), new BigDecimal("126"));

        // Increased Short : -20@126 + -20@127 -10@125
        exec = mock(Execution.class);
        when(exec.getTime()).thenReturn(Instant.ofEpochMilli(70));
        when(exec.getPrice()).thenReturn(new BigDecimal("125"));
        when(exec.getSize()).thenReturn(new BigDecimal("-10"));
        values.add(exec);
        assertNull(target.calculateRecentPrice(context, request, SIGNUM_BUY));
        assertEquals(target.calculateRecentPrice(context, request, SIGNUM_SELL), new BigDecimal("125"));

        // Decreased Short : -20@126 + -20@127 -10@125 + 10 = -10@126 + -20@127 -10@125
        exec = mock(Execution.class);
        when(exec.getTime()).thenReturn(Instant.ofEpochMilli(75));
        when(exec.getPrice()).thenReturn(new BigDecimal("100"));
        when(exec.getSize()).thenReturn(new BigDecimal("10"));
        values.add(exec);
        assertNull(target.calculateRecentPrice(context, request, SIGNUM_BUY));
        assertEquals(target.calculateRecentPrice(context, request, SIGNUM_SELL), new BigDecimal("125"));

        // Decreased Short : -10@126 + -20@127 -10@125 + 20 = -10@127 -10@125
        exec = mock(Execution.class);
        when(exec.getTime()).thenReturn(Instant.ofEpochMilli(80));
        when(exec.getPrice()).thenReturn(new BigDecimal("999"));
        when(exec.getSize()).thenReturn(new BigDecimal("20"));
        values.add(exec);
        assertNull(target.calculateRecentPrice(context, request, SIGNUM_BUY));
        assertEquals(target.calculateRecentPrice(context, request, SIGNUM_SELL), new BigDecimal("125"));

        // Flat : -10@127 -10@125 + 20 = 0
        exec = mock(Execution.class);
        when(exec.getTime()).thenReturn(Instant.ofEpochMilli(85));
        when(exec.getPrice()).thenReturn(new BigDecimal("999"));
        when(exec.getSize()).thenReturn(new BigDecimal("20"));
        values.add(exec);
        assertNull(target.calculateRecentPrice(context, request, SIGNUM_BUY));
        assertNull(target.calculateRecentPrice(context, request, SIGNUM_SELL));

        // New Short : -50@131
        exec = mock(Execution.class);
        when(exec.getTime()).thenReturn(Instant.ofEpochMilli(90));
        when(exec.getPrice()).thenReturn(new BigDecimal("131"));
        when(exec.getSize()).thenReturn(new BigDecimal("-50"));
        values.add(exec);
        assertNull(target.calculateRecentPrice(context, request, SIGNUM_BUY));
        assertEquals(target.calculateRecentPrice(context, request, SIGNUM_SELL), new BigDecimal("131"));

        // Zero duration
        request = rBuilder.tradingDuration(Duration.ofMillis(0)).build();
        assertNull(target.calculateRecentPrice(context, request, SIGNUM_BUY));
        assertNull(target.calculateRecentPrice(context, request, SIGNUM_SELL));

    }

    @Test
    public void testCalculateBuyLossBasis() {

        Request request = rBuilder.build();
        Key key = Key.from(request);

        // 10 bps (no loss)
        BigDecimal market = new BigDecimal("445000");
        BigDecimal recent = new BigDecimal("444000");
        doReturn(market).when(context).getBestBidPrice(key);
        doReturn(recent).when(target).calculateRecentPrice(context, request, SIGNUM_BUY);
        assertEquals(target.calculateBuyLossBasis(context, request).signum(), 0);

        // loss = (446000 - 445000) / 446000 = 0.002242152466368...
        market = new BigDecimal("445000");
        recent = new BigDecimal("446000");
        doReturn(market).when(context).getBestBidPrice(key);
        doReturn(recent).when(target).calculateRecentPrice(context, request, SIGNUM_BUY);
        assertEquals(target.calculateBuyLossBasis(context, request), new BigDecimal("0.0022421525"));

        // With aversion
        request = rBuilder.tradingAversion(new BigDecimal("3")).build();
        doReturn(recent).when(target).calculateRecentPrice(context, request, SIGNUM_BUY);
        assertEquals(target.calculateBuyLossBasis(context, request), new BigDecimal("0.0067264575"));

        // Null market
        doReturn(null).when(context).getBestBidPrice(key);
        doReturn(recent).when(target).calculateRecentPrice(context, request, SIGNUM_BUY);
        assertEquals(target.calculateBuyLossBasis(context, request), ZERO);

        // Null latest
        doReturn(market).when(context).getBestBidPrice(key);
        doReturn(null).when(target).calculateRecentPrice(context, request, SIGNUM_BUY);
        assertEquals(target.calculateBuyLossBasis(context, request), ZERO);

        // Zero latest
        doReturn(market).when(context).getBestBidPrice(key);
        doReturn(new BigDecimal("0.0")).when(target).calculateRecentPrice(context, request, SIGNUM_BUY);
        assertEquals(target.calculateBuyLossBasis(context, request), ZERO);

    }

    @Test
    public void testCalculateBuyBasis() {

        Request request = rBuilder.tradingSpreadBid(new BigDecimal("-0.0005")).build();
        BigDecimal base = new BigDecimal("0.0010");

        doReturn(new BigDecimal("-0.987")).when(target).calculatePositionRatio(context, request);
        doReturn(new BigDecimal("+0.012")).when(target).calculateBuyLossBasis(context, request);
        assertEquals(target.calculateBuyBasis(context, request, base), new BigDecimal("0.0125"));

        doReturn(new BigDecimal("+0.987")).when(target).calculatePositionRatio(context, request);
        doReturn(new BigDecimal("+0.012")).when(target).calculateBuyLossBasis(context, request);
        assertEquals(target.calculateBuyBasis(context, request, base), new BigDecimal("0.0134870"));

        doReturn(null).when(target).calculatePositionRatio(context, request);
        doReturn(new BigDecimal("+0.012")).when(target).calculateBuyLossBasis(context, request);
        assertEquals(target.calculateBuyBasis(context, request, base), new BigDecimal("0.0125"));

        doReturn(new BigDecimal("+0.987")).when(target).calculatePositionRatio(context, request);
        doReturn(null).when(target).calculateBuyLossBasis(context, request);
        assertEquals(target.calculateBuyBasis(context, request, base), new BigDecimal("0.0014870"));

        assertEquals(target.calculateBuyBasis(context, request, null), null);

    }

    @Test
    public void testCalculateSellLossBasis() {

        Request request = rBuilder.build();
        Key key = Key.from(request);

        // 10 bps (no loss)
        BigDecimal market = new BigDecimal("444000");
        BigDecimal recent = new BigDecimal("445000");
        doReturn(market).when(context).getBestAskPrice(key);
        doReturn(recent).when(target).calculateRecentPrice(context, request, SIGNUM_SELL);
        assertEquals(target.calculateSellLossBasis(context, request).signum(), 0);

        // loss = (446000 - 445000) / 445000 = 0.002247191011236...
        market = new BigDecimal("446000");
        recent = new BigDecimal("445000");
        doReturn(market).when(context).getBestAskPrice(key);
        doReturn(recent).when(target).calculateRecentPrice(context, request, SIGNUM_SELL);
        assertEquals(target.calculateSellLossBasis(context, request), new BigDecimal("0.0022471911"));

        // With aversion
        request = rBuilder.tradingAversion(new BigDecimal("3")).build();
        doReturn(recent).when(target).calculateRecentPrice(context, request, SIGNUM_SELL);
        assertEquals(target.calculateSellLossBasis(context, request), new BigDecimal("0.0067415733"));

        // Null market
        doReturn(null).when(context).getBestAskPrice(key);
        doReturn(recent).when(target).calculateRecentPrice(context, request, SIGNUM_SELL);
        assertEquals(target.calculateSellLossBasis(context, request), ZERO);

        // Null latest
        doReturn(market).when(context).getBestAskPrice(key);
        doReturn(null).when(target).calculateRecentPrice(context, request, SIGNUM_SELL);
        assertEquals(target.calculateSellLossBasis(context, request), ZERO);

        // Zero latest
        doReturn(market).when(context).getBestAskPrice(key);
        doReturn(new BigDecimal("0.0")).when(target).calculateRecentPrice(context, request, SIGNUM_SELL);
        assertEquals(target.calculateSellLossBasis(context, request), ZERO);

    }

    @Test
    public void testCalculateSellBasis() {

        Request request = rBuilder.tradingSpreadAsk(new BigDecimal("-0.0005")).build();
        BigDecimal base = new BigDecimal("0.0010");

        doReturn(new BigDecimal("+0.987")).when(target).calculatePositionRatio(context, request);
        doReturn(new BigDecimal("+0.012")).when(target).calculateSellLossBasis(context, request);
        assertEquals(target.calculateSellBasis(context, request, base), new BigDecimal("0.0125"));

        doReturn(new BigDecimal("-0.987")).when(target).calculatePositionRatio(context, request);
        doReturn(new BigDecimal("+0.012")).when(target).calculateSellLossBasis(context, request);
        assertEquals(target.calculateSellBasis(context, request, base), new BigDecimal("0.0134870"));

        doReturn(null).when(target).calculatePositionRatio(context, request);
        doReturn(new BigDecimal("+0.012")).when(target).calculateSellLossBasis(context, request);
        assertEquals(target.calculateSellBasis(context, request, base), new BigDecimal("0.0125"));

        doReturn(new BigDecimal("-0.987")).when(target).calculatePositionRatio(context, request);
        doReturn(null).when(target).calculateSellLossBasis(context, request);
        assertEquals(target.calculateSellBasis(context, request, base), new BigDecimal("0.0014870"));

        assertEquals(target.calculateSellBasis(context, request, null), null);

    }

    @Test
    public void testCalculateBuyBoundaryPrice() {

        Request request = rBuilder.build();
        Key key = Key.from(request);
        doReturn(new BigDecimal("0.0001")).when(target).calculateBasis(context, request);

        // Normal
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("16000.0000"));
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("15000.0000"));
        doReturn(null).when(target).calculateRecentPrice(context, request, SIGNUM_SELL);
        assertEquals(target.calculateBuyBoundaryPrice(context, request), new BigDecimal("15000.0025"));

        // Equal
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("15000.0000"));
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("15000.0000"));
        doReturn(null).when(target).calculateRecentPrice(context, request, SIGNUM_SELL);
        assertEquals(target.calculateBuyBoundaryPrice(context, request), new BigDecimal("14999.9975"));

        // Inverse
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("15000.0000"));
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("16000.0000"));
        doReturn(null).when(target).calculateRecentPrice(context, request, SIGNUM_SELL);
        assertEquals(target.calculateBuyBoundaryPrice(context, request), new BigDecimal("14999.9975"));

        // Null Bid
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("15000.0000"));
        when(context.getBestBidPrice(key)).thenReturn(null);
        doReturn(null).when(target).calculateRecentPrice(context, request, SIGNUM_SELL);
        assertEquals(target.calculateBuyBoundaryPrice(context, request), new BigDecimal("14999.9975"));

        // Null Ask
        when(context.getBestAskPrice(key)).thenReturn(null);
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("15000.0000"));
        doReturn(null).when(target).calculateRecentPrice(context, request, SIGNUM_SELL);
        assertEquals(target.calculateBuyBoundaryPrice(context, request), null);

        // Losing unwind
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("16000.0000"));
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("15000.0000"));
        doReturn(valueOf(14500)).when(target).calculateRecentPrice(context, request, SIGNUM_SELL);
        assertEquals(target.calculateBuyBoundaryPrice(context, request), new BigDecimal("14497.1000"));

        // Already at BBO
        Order order = mock(Order.class);
        when(order.getOrderPrice()).thenReturn(valueOf(15000));
        when(order.getOrderQuantity()).thenReturn(ONE);
        when(context.listActiveOrders(key)).thenReturn(singletonList(order));
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("16000.0000"));
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("15000.0000"));
        doReturn(null).when(target).calculateRecentPrice(context, request, SIGNUM_SELL);
        assertEquals(target.calculateBuyBoundaryPrice(context, request), new BigDecimal("15000.0000"));

    }

    @Test
    public void testCalculateSellBoundaryPrice() {

        Request request = rBuilder.build();
        Key key = Key.from(request);
        doReturn(new BigDecimal("0.0001")).when(target).calculateBasis(context, request);

        // Normal
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("15000.0000"));
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("14000.0000"));
        doReturn(null).when(target).calculateRecentPrice(context, request, SIGNUM_BUY);
        assertEquals(target.calculateSellBoundaryPrice(context, request), new BigDecimal("14999.9975"));

        // Equal
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("15000.0000"));
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("15000.0000"));
        doReturn(null).when(target).calculateRecentPrice(context, request, SIGNUM_BUY);
        assertEquals(target.calculateSellBoundaryPrice(context, request), new BigDecimal("15000.0025"));

        // Inverse
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("15000.0000"));
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("16000.0000"));
        doReturn(null).when(target).calculateRecentPrice(context, request, SIGNUM_BUY);
        assertEquals(target.calculateSellBoundaryPrice(context, request), new BigDecimal("16000.0025"));

        // Null Bid
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("15000.0000"));
        when(context.getBestBidPrice(key)).thenReturn(null);
        doReturn(null).when(target).calculateRecentPrice(context, request, SIGNUM_BUY);
        assertEquals(target.calculateSellBoundaryPrice(context, request), null);

        // Null Ask
        when(context.getBestAskPrice(key)).thenReturn(null);
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("15000.0000"));
        doReturn(null).when(target).calculateRecentPrice(context, request, SIGNUM_BUY);
        assertEquals(target.calculateSellBoundaryPrice(context, request), new BigDecimal("15000.0025"));

        // Losing unwind
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("15000.0000"));
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("14000.0000"));
        doReturn(valueOf(15500)).when(target).calculateRecentPrice(context, request, SIGNUM_BUY);
        assertEquals(target.calculateSellBoundaryPrice(context, request), new BigDecimal("15503.1000"));

        // Already at BBO
        Order order = mock(Order.class);
        when(order.getOrderPrice()).thenReturn(valueOf(15000));
        when(order.getOrderQuantity()).thenReturn(ONE.negate());
        when(context.listActiveOrders(key)).thenReturn(singletonList(order));
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("15000.0000"));
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("14000.0000"));
        doReturn(null).when(target).calculateRecentPrice(context, request, SIGNUM_BUY);
        assertEquals(target.calculateSellBoundaryPrice(context, request), new BigDecimal("15000.0000"));

    }

    @Test
    public void testCalculateBuyLimitPrice() throws Exception {

        Request request = rBuilder.build();

        BigDecimal weighed = new BigDecimal("12347.7777");
        BigDecimal basis = new BigDecimal("0.0096");

        // Target : 12347.7777 * (1 - 96 bps) = 12229.23903408
        // Cross : min(12229.23903408, 20000)
        // Rounded : 12229.2375
        doReturn(new BigDecimal("20000")).when(target).calculateBuyBoundaryPrice(context, request);
        BigDecimal result = target.calculateBuyLimitPrice(context, request, weighed, basis);
        assertEquals(result, new BigDecimal("12229.2375"));

        // Cross Protected
        doReturn(new BigDecimal("9999.9975")).when(target).calculateBuyBoundaryPrice(context, request);
        result = target.calculateBuyLimitPrice(context, request, weighed, basis);
        assertEquals(result, new BigDecimal("9999.9975"));

        // Invalid Input
        assertNull(target.calculateBuyLimitPrice(context, request, null, basis));
        assertNull(target.calculateBuyLimitPrice(context, request, weighed, null));

        // Null Bound
        doReturn(null).when(target).calculateBuyBoundaryPrice(context, request);
        assertNull(target.calculateBuyLimitPrice(context, request, weighed, basis));

    }

    @Test
    public void testCalculateSellLimitPrice() throws Exception {

        Request request = rBuilder.build();

        BigDecimal weighed = new BigDecimal("12347.7777");
        BigDecimal basis = new BigDecimal("0.0096");

        // Target : 12347.7777 * (1 + 96 bps) = 12466.31636592
        // Cross : max(12466.31636592, 10000)
        // Rounded : 12466.3175
        doReturn(new BigDecimal("10000")).when(target).calculateSellBoundaryPrice(context, request);
        BigDecimal result = target.calculateSellLimitPrice(context, request, weighed, basis);
        assertEquals(result, new BigDecimal("12466.3175"));

        // Cross Protected
        doReturn(new BigDecimal("20000.0025")).when(target).calculateSellBoundaryPrice(context, request);
        result = target.calculateSellLimitPrice(context, request, weighed, basis);
        assertEquals(result, new BigDecimal("20000.0025"));

        // Invalid Input
        assertNull(target.calculateSellLimitPrice(context, request, null, basis));
        assertNull(target.calculateSellLimitPrice(context, request, weighed, null));

        // Null Bound
        doReturn(null).when(target).calculateSellBoundaryPrice(context, request);
        assertNull(target.calculateSellLimitPrice(context, request, weighed, basis));

    }

    @Test
    public void testCalculateFundingExposureSize() {

        Request.RequestBuilder builder = rBuilder.tradingExposure(new BigDecimal("0.10"));
        when(context.getFundingPosition(any())).thenReturn(new BigDecimal("18000"));
        BigDecimal price = new BigDecimal("123.4567");

        // Exposed = Fund * Exposure = 900
        // Exposed / Price = 900 / 123.4567 = 7.290005322...
        BigDecimal expect = new BigDecimal("7.290005321700");
        assertEquals(target.calculateFundingExposureSize(context, builder.build(), price), expect);

        // Leveraged
        // Exposed = Fund * (1 + 5) * Exposure = 10800
        // Exposed / Price = 10800 / 123.4567 = 87.4800638...
        builder = builder.fundingOffset(BigDecimal.valueOf(5));
        expect = new BigDecimal("87.480063860450");
        assertEquals(target.calculateFundingExposureSize(context, builder.build(), price), expect);

        // Leveraged (Over)
        // Exposed = Fund * (1 + 100) * Exposure = 180000
        // Exposed / Price = 180000 / 123.4567 = 1,458.00106434...
        // Actual Fund / Price = 18000 / 123.4567 = 145.800106434077697
        builder = builder.fundingOffset(BigDecimal.valueOf(100));
        expect = new BigDecimal("145.8001064340");
        assertEquals(target.calculateFundingExposureSize(context, builder.build(), price), expect);

        // Invalid price
        assertEquals(target.calculateFundingExposureSize(context, builder.build(), null), ZERO);
        assertEquals(target.calculateFundingExposureSize(context, builder.build(), ZERO), ZERO);

        // Invalid Fund
        when(context.getFundingPosition(any())).thenReturn(null);
        assertEquals(target.calculateFundingExposureSize(context, builder.build(), price), ZERO);

    }

    @Test
    public void testCalculateInstrumentExposureSize() {

        Request request = rBuilder.tradingExposure(new BigDecimal("0.10")).build();
        Key key = Key.from(request);

        // Exposed = Instrument * Exposure = 900
        BigDecimal expect = new BigDecimal("900.00");
        when(context.getInstrumentPosition(key)).thenReturn(new BigDecimal("9000"));
        assertEquals(target.calculateInstrumentExposureSize(context, request), expect);

        // Invalid Fund
        when(context.getInstrumentPosition(key)).thenReturn(null);
        assertEquals(target.calculateInstrumentExposureSize(context, request), ZERO);

    }

    @Test
    public void testCalculateBuyLimitSize_Cash() throws Exception {

        Request request = rBuilder.build();
        Key key = Key.from(request);
        BigDecimal price = new BigDecimal("123.45");
        when(context.isMarginable(key)).thenReturn(null);

        // Net Short 1 (123.00 -> 123.00)
        doReturn(new BigDecimal("123")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("0")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateBuyLimitSize(context, request, price), new BigDecimal("123.00"));

        // Net Short 2 (98.40 -> 98.50)
        doReturn(new BigDecimal("98.4")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("24.6")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateBuyLimitSize(context, request, price), new BigDecimal("98.50"));

        // Equal (61.50 -> 61.50)
        doReturn(new BigDecimal("61.5")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("61.5")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateBuyLimitSize(context, request, price), new BigDecimal("61.50"));

        // Net Long 1 (17.22 -> 17.25)
        doReturn(new BigDecimal("24.6")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("98.4")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateBuyLimitSize(context, request, price), new BigDecimal("17.25"));

        // Net Long 2 (0.00 -> 0.00)
        doReturn(new BigDecimal("0")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("123")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateBuyLimitSize(context, request, price), new BigDecimal("0.00"));

    }

    @Test
    public void testCalculateBuyLimitSize_Margin() throws Exception {

        Request request = rBuilder.build();
        Key key = Key.from(request);
        BigDecimal price = new BigDecimal("123.45");
        when(context.isMarginable(key)).thenReturn(true);

        // Net Short 1 (61.50 -> 61.50)
        doReturn(new BigDecimal("0")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("-123")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateBuyLimitSize(context, request, price), new BigDecimal("61.50"));

        // Net Short 2 (61.50 -> 61.50)
        doReturn(new BigDecimal("+61.5")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("-61.5")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateBuyLimitSize(context, request, price), new BigDecimal("61.50"));

        // Net Short 3 (61.50 -> 61.50)
        doReturn(new BigDecimal("+73.8")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("-49.2")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateBuyLimitSize(context, request, price), new BigDecimal("61.50"));

        // Equal (61.50 -> 61.50)
        doReturn(new BigDecimal("123")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("0")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateBuyLimitSize(context, request, price), new BigDecimal("61.50"));

        // Net Long 1 (12.30 -> 12.25)
        doReturn(new BigDecimal("73.8")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("49.2")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateBuyLimitSize(context, request, price), new BigDecimal("12.25"));

        // Net Long 2 (0.00 -> 0.00)
        doReturn(new BigDecimal("61.5")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("61.5")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateBuyLimitSize(context, request, price), new BigDecimal("0.00"));

        // Net Long 3 (0.00 -> 0.00)
        doReturn(new BigDecimal("0")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("123")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateBuyLimitSize(context, request, price), new BigDecimal("0.00"));

    }

    @Test
    public void testCalculateSellLimitSize_Cash() throws Exception {

        Request request = rBuilder.build();
        Key key = Key.from(request);
        BigDecimal price = new BigDecimal("123.45");
        when(context.isMarginable(key)).thenReturn(null);

        // Net Short 1 (0.00 -> 0.00)
        doReturn(new BigDecimal("123")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("0")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateSellLimitSize(context, request, price), new BigDecimal("0.00"));

        // Net Short 2 (17.22 -> 17.25)
        doReturn(new BigDecimal("98.4")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("24.6")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateSellLimitSize(context, request, price), new BigDecimal("17.25"));

        // Equal (61.50 -> 61.50)
        doReturn(new BigDecimal("61.5")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("61.5")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateSellLimitSize(context, request, price), new BigDecimal("61.50"));

        // Net Long 1 (98.40 -> 98.50)
        doReturn(new BigDecimal("24.6")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("98.4")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateSellLimitSize(context, request, price), new BigDecimal("98.50"));

        // Net Long 2 (123.00 -> 123.00)
        doReturn(new BigDecimal("0")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("123")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateSellLimitSize(context, request, price), new BigDecimal("123.00"));

    }

    @Test
    public void testCalculateSellLimitSize_Margin() throws Exception {

        Request request = rBuilder.build();
        Key key = Key.from(request);
        BigDecimal price = new BigDecimal("123.45");
        when(context.isMarginable(key)).thenReturn(true);

        // Net Short 1 (0.00 -> 0.00)
        doReturn(new BigDecimal("0")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("-123")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateSellLimitSize(context, request, price), new BigDecimal("0.00"));

        // Net Short 2 (0.00 -> 0.00)
        doReturn(new BigDecimal("+61.5")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("-61.5")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateSellLimitSize(context, request, price), new BigDecimal("0.00"));

        // Net Short 3 (12.30 -> 12.25)
        doReturn(new BigDecimal("+73.8")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("-49.2")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateSellLimitSize(context, request, price), new BigDecimal("12.25"));

        // Equal (61.50 -> 61.50)
        doReturn(new BigDecimal("123")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("0")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateSellLimitSize(context, request, price), new BigDecimal("61.50"));

        // Net Long 1 (61.50 -> 61.50)
        doReturn(new BigDecimal("73.8")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("49.2")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateSellLimitSize(context, request, price), new BigDecimal("61.50"));

        // Net Long 2 (61.50 -> 61.50)
        doReturn(new BigDecimal("61.5")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("61.5")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateSellLimitSize(context, request, price), new BigDecimal("61.50"));

        // Net Long 3 (61.50 -> 61.50)
        doReturn(new BigDecimal("0")).when(target).calculateFundingExposureSize(context, request, price);
        doReturn(new BigDecimal("123")).when(target).calculateInstrumentExposureSize(context, request);
        assertEquals(target.calculateSellLimitSize(context, request, price), new BigDecimal("61.50"));

    }

}
