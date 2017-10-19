package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

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
    public void testCollapsePrices() throws Exception {

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
        NavigableMap<Instant, BigDecimal> result = target.collapsePrices(trades, interval, fromTime, toTime);
        assertEquals(result.size(), 12);
        assertEquals(result.remove(Instant.ofEpochMilli(10990)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(10994)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(10998)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(11002)), new BigDecimal("1001.5024630542"));
        assertEquals(result.remove(Instant.ofEpochMilli(11006)), new BigDecimal("1004.5119617225"));
        assertEquals(result.remove(Instant.ofEpochMilli(11010)), new BigDecimal("1008.5115207373"));
        assertEquals(result.remove(Instant.ofEpochMilli(11014)), new BigDecimal("1012.5111111111"));
        assertEquals(result.remove(Instant.ofEpochMilli(11018)), new BigDecimal("1016.5107296137"));
        assertEquals(result.remove(Instant.ofEpochMilli(11022)), new BigDecimal("1019.5020920502"));
        assertEquals(result.remove(Instant.ofEpochMilli(11026)), new BigDecimal("1019.5020920502"));
        assertEquals(result.remove(Instant.ofEpochMilli(11030)), new BigDecimal("1019.5020920502"));
        assertEquals(result.remove(Instant.ofEpochMilli(11034)), new BigDecimal("1019.5020920502"));
        assertEquals(result.size(), 0, result.toString());

        // Remove a bucket
        when(trades.get(6).getTimestamp()).thenReturn(Instant.ofEpochMilli(10990));
        when(trades.get(7).getTimestamp()).thenReturn(Instant.ofEpochMilli(11035));
        when(trades.get(8).getPrice()).thenReturn(null);
        when(trades.get(9).getSize()).thenReturn(null);
        result = target.collapsePrices(trades, interval, fromTime, toTime);
        assertEquals(result.size(), 12);
        assertEquals(result.remove(Instant.ofEpochMilli(10990)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(10994)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(10998)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(11002)), new BigDecimal("1001.5024630542"));
        assertEquals(result.remove(Instant.ofEpochMilli(11006)), new BigDecimal("1004.5119617225"));
        assertEquals(result.remove(Instant.ofEpochMilli(11010)), new BigDecimal("1004.5119617225"));
        assertEquals(result.remove(Instant.ofEpochMilli(11014)), new BigDecimal("1012.5111111111"));
        assertEquals(result.remove(Instant.ofEpochMilli(11018)), new BigDecimal("1016.5107296137"));
        assertEquals(result.remove(Instant.ofEpochMilli(11022)), new BigDecimal("1019.5020920502"));
        assertEquals(result.remove(Instant.ofEpochMilli(11026)), new BigDecimal("1019.5020920502"));
        assertEquals(result.remove(Instant.ofEpochMilli(11030)), new BigDecimal("1019.5020920502"));
        assertEquals(result.remove(Instant.ofEpochMilli(11034)), new BigDecimal("1019.5020920502"));
        assertEquals(result.size(), 0, result.toString());

        // Invalidate bucket
        when(trades.get(9).getSize()).thenReturn(BigDecimal.ZERO);
        result = target.collapsePrices(trades, interval, fromTime, toTime);
        assertEquals(result.size(), 12);
        assertEquals(result.remove(Instant.ofEpochMilli(10990)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(10994)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(10998)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(11002)), new BigDecimal("1001.5024630542"));
        assertEquals(result.remove(Instant.ofEpochMilli(11006)), new BigDecimal("1004.5119617225"));
        assertEquals(result.remove(Instant.ofEpochMilli(11010)), new BigDecimal("1004.5119617225"));
        assertEquals(result.remove(Instant.ofEpochMilli(11014)), new BigDecimal("1012.5111111111"));
        assertEquals(result.remove(Instant.ofEpochMilli(11018)), new BigDecimal("1016.5107296137"));
        assertEquals(result.remove(Instant.ofEpochMilli(11022)), new BigDecimal("1019.5020920502"));
        assertEquals(result.remove(Instant.ofEpochMilli(11026)), new BigDecimal("1019.5020920502"));
        assertEquals(result.remove(Instant.ofEpochMilli(11030)), new BigDecimal("1019.5020920502"));
        assertEquals(result.remove(Instant.ofEpochMilli(11034)), new BigDecimal("1019.5020920502"));
        assertEquals(result.size(), 0, result.toString());

    }

    @Test
    public void testCalculateReturns() throws Exception {

        SortedMap<Instant, BigDecimal> prices = new TreeMap<>();
        prices.put(Instant.ofEpochMilli(11002), new BigDecimal("1001.5024630542"));
        prices.put(Instant.ofEpochMilli(11006), new BigDecimal("1004.5119617225"));
        prices.put(Instant.ofEpochMilli(11010), new BigDecimal("1008.5115207373"));
        prices.put(Instant.ofEpochMilli(11014), new BigDecimal("1012.5111111111"));
        prices.put(Instant.ofEpochMilli(11018), new BigDecimal("1016.5107296137"));
        prices.put(Instant.ofEpochMilli(11022), new BigDecimal("1019.5020920502"));
        prices.put(Instant.ofEpochMilli(11026), new BigDecimal("1019.5020920502"));
        prices.put(Instant.ofEpochMilli(11030), new BigDecimal("1019.5020920502"));
        prices.put(Instant.ofEpochMilli(11034), new BigDecimal("1019.5020920502"));

        NavigableMap<Instant, BigDecimal> result = target.calculateReturns(prices);
        assertEquals(result.size(), prices.size() - 1);
        assertEquals(result.remove(Instant.ofEpochMilli(11006)), new BigDecimal("0.0030004779"));
        assertEquals(result.remove(Instant.ofEpochMilli(11010)), new BigDecimal("0.0039736886"));
        assertEquals(result.remove(Instant.ofEpochMilli(11014)), new BigDecimal("0.0039579919"));
        assertEquals(result.remove(Instant.ofEpochMilli(11018)), new BigDecimal("0.0039424156"));
        assertEquals(result.remove(Instant.ofEpochMilli(11022)), new BigDecimal("0.0029384536"));
        assertEquals(result.remove(Instant.ofEpochMilli(11026)), new BigDecimal("0.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11030)), new BigDecimal("0.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11034)), new BigDecimal("0.0000000000"));
        assertEquals(result.size(), 0, result.toString());

        // Missing Price
        prices.put(Instant.ofEpochMilli(11010), null);
        result = target.calculateReturns(prices);
        assertEquals(result.size(), prices.size() - 1);
        assertEquals(result.remove(Instant.ofEpochMilli(11006)), new BigDecimal("0.0030004779"));
        assertEquals(result.remove(Instant.ofEpochMilli(11010)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(11014)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(11018)), new BigDecimal("0.0039424156"));
        assertEquals(result.remove(Instant.ofEpochMilli(11022)), new BigDecimal("0.0029384536"));
        assertEquals(result.remove(Instant.ofEpochMilli(11026)), new BigDecimal("0.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11030)), new BigDecimal("0.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11034)), new BigDecimal("0.0000000000"));
        assertEquals(result.size(), 0, result.toString());

        // Zero Price
        prices.put(Instant.ofEpochMilli(11010), new BigDecimal("0.0"));
        result = target.calculateReturns(prices);
        assertEquals(result.size(), prices.size() - 1);
        assertEquals(result.remove(Instant.ofEpochMilli(11006)), new BigDecimal("0.0030004779"));
        assertEquals(result.remove(Instant.ofEpochMilli(11010)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(11014)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(11018)), new BigDecimal("0.0039424156"));
        assertEquals(result.remove(Instant.ofEpochMilli(11022)), new BigDecimal("0.0029384536"));
        assertEquals(result.remove(Instant.ofEpochMilli(11026)), new BigDecimal("0.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11030)), new BigDecimal("0.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11034)), new BigDecimal("0.0000000000"));
        assertEquals(result.size(), 0, result.toString());

        // Null arg
        assertEquals(target.calculateReturns(null).size(), 0);

    }

}
