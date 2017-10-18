package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class AbstractEstimatorTest {

    static class TestEstimator extends AbstractEstimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return BAIL;
        }
    }

    private AbstractEstimator target;

    @BeforeMethod
    public void setUp() throws Exception {
        target = new TestEstimator();
    }

    @Test
    public void testGetKey() throws Exception {

        Request r = Request.builder().build();

        assertEquals(target.getKey(r), Context.Key.from(r));

    }

    @Test
    public void testCalculateReturns() throws Exception {

        List<Trade> trades = new ArrayList<>();

        for (int i = 1; i <= 20; i++) {
            Trade t = mock(Trade.class);
            when(t.getTimestamp()).thenReturn(Instant.ofEpochMilli(i + 11000));
            when(t.getPrice()).thenReturn(BigDecimal.valueOf(i + 1000));
            when(t.getSize()).thenReturn(BigDecimal.valueOf(i + 100));
            trades.add(t);
        }

        Duration interval = Duration.ofMillis(4);
        Instant fromTime = Instant.ofEpochMilli(10990);
        Instant toTime = Instant.ofEpochMilli(11035);
        NavigableMap<Instant, BigDecimal> result = target.calculateReturns(trades, interval, fromTime, toTime);
        assertEquals(result.size(), 11);
        assertEquals(result.remove(Instant.ofEpochMilli(10994)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(10998)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(11002)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(11006)), new BigDecimal("0.0030004779"));
        assertEquals(result.remove(Instant.ofEpochMilli(11010)), new BigDecimal("0.0039736886"));
        assertEquals(result.remove(Instant.ofEpochMilli(11014)), new BigDecimal("0.0039579919"));
        assertEquals(result.remove(Instant.ofEpochMilli(11018)), new BigDecimal("0.0039424156"));
        assertEquals(result.remove(Instant.ofEpochMilli(11022)), new BigDecimal("0.0029384536"));
        assertEquals(result.remove(Instant.ofEpochMilli(11026)), new BigDecimal("0.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11030)), new BigDecimal("0.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11034)), new BigDecimal("0.0000000000"));
        assertEquals(result.size(), 0);

        // Remove a bucket
        when(trades.get(6).getTimestamp()).thenReturn(Instant.ofEpochMilli(10990));
        when(trades.get(7).getTimestamp()).thenReturn(Instant.ofEpochMilli(11035));
        when(trades.get(8).getPrice()).thenReturn(null);
        when(trades.get(9).getSize()).thenReturn(null);
        result = target.calculateReturns(trades, interval, fromTime, toTime);
        assertEquals(result.size(), 11);
        assertEquals(result.remove(Instant.ofEpochMilli(10994)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(10998)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(11002)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(11006)), new BigDecimal("0.0030004779"));
        assertEquals(result.remove(Instant.ofEpochMilli(11010)), new BigDecimal("0.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11014)), new BigDecimal("0.0079316805"));
        assertEquals(result.remove(Instant.ofEpochMilli(11018)), new BigDecimal("0.0039424156"));
        assertEquals(result.remove(Instant.ofEpochMilli(11022)), new BigDecimal("0.0029384536"));
        assertEquals(result.remove(Instant.ofEpochMilli(11026)), new BigDecimal("0.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11030)), new BigDecimal("0.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11034)), new BigDecimal("0.0000000000"));
        assertEquals(result.size(), 0);

        // Invalidate bucket
        when(trades.get(9).getSize()).thenReturn(BigDecimal.ZERO);
        result = target.calculateReturns(trades, interval, fromTime, toTime);
        assertEquals(result.size(), 11);
        assertEquals(result.remove(Instant.ofEpochMilli(10994)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(10998)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(11002)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(11006)), new BigDecimal("0.0030004779"));
        assertEquals(result.remove(Instant.ofEpochMilli(11010)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(11014)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(11018)), new BigDecimal("0.0039424156"));
        assertEquals(result.remove(Instant.ofEpochMilli(11022)), new BigDecimal("0.0029384536"));
        assertEquals(result.remove(Instant.ofEpochMilli(11026)), new BigDecimal("0.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11030)), new BigDecimal("0.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11034)), new BigDecimal("0.0000000000"));
        assertEquals(result.size(), 0);

    }

}
