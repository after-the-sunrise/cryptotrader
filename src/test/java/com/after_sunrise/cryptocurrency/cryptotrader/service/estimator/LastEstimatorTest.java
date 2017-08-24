package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static java.math.BigDecimal.*;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.*;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.math.NumberUtils.LONG_ONE;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class LastEstimatorTest {

    private LastEstimator target;

    private Context context;

    @BeforeMethod
    public void setUp() throws Exception {

        context = mock(Context.class);

        target = spy(new LastEstimator());

    }

    @Test
    public void testGet() throws Exception {
        assertEquals(target.get(), target.getClass().getSimpleName());
    }

    @Test
    public void testEstimate() throws Exception {

        Instant time = Instant.now();
        doReturn(time).when(target).getNow();
        doReturn(TEN).when(target).calculateConfidence(time.minusSeconds(1), time);

        Trade t1 = mock(Trade.class);
        Trade t2 = mock(Trade.class);
        Trade t3 = mock(Trade.class);
        Trade t4 = mock(Trade.class);
        Trade t5 = mock(Trade.class);
        when(t1.getTimestamp()).thenReturn(time.minusSeconds(1));
        when(t2.getTimestamp()).thenReturn(null);
        when(t3.getTimestamp()).thenReturn(time.minusSeconds(3));
        when(t4.getTimestamp()).thenReturn(time.minusSeconds(4));
        when(t5.getTimestamp()).thenReturn(time.minusSeconds(5));
        when(t1.getPrice()).thenReturn(BigDecimal.valueOf(3));
        when(t2.getPrice()).thenReturn(BigDecimal.valueOf(4));
        when(t3.getPrice()).thenReturn(BigDecimal.valueOf(5));
        when(t4.getPrice()).thenReturn(null);
        when(t5.getPrice()).thenReturn(BigDecimal.valueOf(2));

        Request request = Request.builder().site("s").instrument("i").targetTime(now()).build();
        Key key = Key.from(request);
        List<Trade> trades = asList(t3, t2, null, t5, null, t1, t4);
        when(context.listTrades(key, time.minus(LONG_ONE, DAYS))).thenReturn(trades);

        // Estimated
        Estimation result = target.estimate(context, request);
        assertEquals(result.getPrice(), t1.getPrice());
        assertEquals(result.getConfidence(), TEN);

        // Invalid Request
        result = target.estimate(context, null);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), ZERO);

        // Null trades
        when(context.listTrades(key, time.minus(LONG_ONE, DAYS))).thenReturn(null);
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), ZERO);

        // Invalid trades
        when(context.listTrades(key, time.minus(LONG_ONE, DAYS))).thenReturn(asList((Trade) null));
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), ZERO);

    }

    @Test
    public void testGetNow() throws InterruptedException {

        Instant t1 = target.getNow();

        Thread.sleep(100L);

        assertNotEquals(target.getNow(), t1);

    }

    @Test
    public void testCalculateConfidence() {

        Instant now = Instant.now();

        // No diff
        assertEquals(target.calculateConfidence(now, now), ONE);

        // Future date
        assertEquals(target.calculateConfidence(now.plusSeconds(1), now), ONE);

        // Seconds
        assertEquals(target.calculateConfidence(now.minusSeconds(01), now), new BigDecimal("0.81222774"));
        assertEquals(target.calculateConfidence(now.minusSeconds(10), now), new BigDecimal("0.74963699"));
        assertEquals(target.calculateConfidence(now.minusSeconds(20), now), new BigDecimal("0.73079530"));
        assertEquals(target.calculateConfidence(now.minusSeconds(30), now), new BigDecimal("0.71977361"));
        assertEquals(target.calculateConfidence(now.minusSeconds(40), now), new BigDecimal("0.71195360"));
        assertEquals(target.calculateConfidence(now.minusSeconds(50), now), new BigDecimal("0.70588793"));

        // Minutes
        assertEquals(target.calculateConfidence(now.minus(01, MINUTES), now), new BigDecimal("0.70093192"));
        assertEquals(target.calculateConfidence(now.minus(05, MINUTES), now), new BigDecimal("0.65718286"));
        assertEquals(target.calculateConfidence(now.minus(15, MINUTES), now), new BigDecimal("0.62731948"));
        assertEquals(target.calculateConfidence(now.minus(30, MINUTES), now), new BigDecimal("0.60847779"));
        assertEquals(target.calculateConfidence(now.minus(45, MINUTES), now), new BigDecimal("0.59745610"));

        // Hours
        assertEquals(target.calculateConfidence(now.minus(1, HOURS), now), new BigDecimal("0.58963610"));
        assertEquals(target.calculateConfidence(now.minus(4, HOURS), now), new BigDecimal("0.55195271"));
        assertEquals(target.calculateConfidence(now.minus(8, HOURS), now), new BigDecimal("0.53311101"));
        assertEquals(target.calculateConfidence(now.minus(16, HOURS), now), new BigDecimal("0.51426932"));

        // Days
        assertEquals(target.calculateConfidence(now.minus(1, DAYS), now), new BigDecimal("0.50324764"));
        assertEquals(target.calculateConfidence(now.minus(2, DAYS), now), new BigDecimal("0.48440594"));
        assertEquals(target.calculateConfidence(now.minus(7, DAYS), now), new BigDecimal("0.45035231"));
        assertEquals(target.calculateConfidence(now.minus(30, DAYS), now), new BigDecimal("0.41079350"));
        assertEquals(target.calculateConfidence(now.minus(365, DAYS), now), new BigDecimal("0.34287180"));
        assertEquals(target.calculateConfidence(now.minus(3650, DAYS), now), new BigDecimal("0.28028105"));
        assertEquals(target.calculateConfidence(now.minus(365000, DAYS), now), new BigDecimal("0.15509954"));

    }

}
