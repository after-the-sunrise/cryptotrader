package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.Composite;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import org.apache.commons.configuration2.MapConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class AbstractServiceTest {

    private AbstractService target;

    @BeforeMethod
    public void setUp() throws Exception {
        target = new AbstractService() {
            @Override
            public String get() {
                return "test";
            }
        };
    }

    @Test
    public void testConfiguration() {

        Map<String, Object> map = new HashMap<>();
        target.setConfiguration(new MapConfiguration(map));

        // String
        assertEquals(target.getStringProperty("string", "b"), "b");
        map.put(target.getClass().getName() + ".string", "a");
        assertEquals(target.getStringProperty("string", "b"), "a");
        map.put(target.getClass().getName() + ".string", new Object() {
            @Override
            public String toString() {
                throw new RuntimeException("test");
            }
        }); // Exception
        assertEquals(target.getStringProperty("string", "b"), "b");

        // Int
        assertEquals(target.getIntProperty("int", -123), -123);
        map.put(target.getClass().getName() + ".int", -999);
        assertEquals(target.getIntProperty("int", -123), -999);
        map.put(target.getClass().getName() + ".int", "a"); // Exception
        assertEquals(target.getIntProperty("int", -123), -123);

        // Long
        assertEquals(target.getLongProperty("int", -123), -123);
        map.put(target.getClass().getName() + ".int", -999);
        assertEquals(target.getLongProperty("int", -123), -999);
        map.put(target.getClass().getName() + ".int", "a"); // Exception
        assertEquals(target.getLongProperty("int", -123), -123);

        // Decimal
        assertEquals(target.getDecimalProperty("decimal", TEN), TEN);
        map.put(target.getClass().getName() + ".decimal", "1");
        assertEquals(target.getDecimalProperty("decimal", TEN), ONE);
        map.put(target.getClass().getName() + ".decimal", "a"); // Exception
        assertEquals(target.getDecimalProperty("decimal", TEN), TEN);


    }

    @Test
    public void testCalculateComposite() {

        class TestFunction implements BiFunction<String, String, BigDecimal> {
            @Override
            public BigDecimal apply(String s, String s2) {
                return null;
            }
        }

        BiFunction<String, String, BigDecimal> f = spy(new TestFunction());

        List<Composite> products = new ArrayList<>();

        Runnable initializer = () -> {
            products.clear();
            products.add(new Composite("*s1", "p1"));
            products.add(new Composite("*s1", "p2"));
            products.add(new Composite("/s1", "p3"));
            products.add(new Composite("*s2", "p4"));
            products.add(new Composite("@s2", "p4"));
            products.add(new Composite("@s2", "p5"));

            reset(f);
            when(f.apply("s1", "p1")).thenReturn(new BigDecimal("1.2"));
            when(f.apply("s1", "p2")).thenReturn(new BigDecimal("2.3"));
            when(f.apply("s1", "p3")).thenReturn(new BigDecimal("3.4"));
            when(f.apply("s2", "p4")).thenReturn(new BigDecimal("4.5"));
            when(f.apply("s2", "p5")).thenReturn(new BigDecimal("5.6"));
        };

        // 1 * 1.2 * 2.3 / 3.4 * 4.5 = 3.652941176470588
        // Average = (3.652941176470588 + 4.5 + 5.6) / 3 = 4.584313725490196
        initializer.run();
        assertEquals(target.calculateComposite(products, f), new BigDecimal("4.5843137255"));

        // Average Only
        initializer.run();
        products.clear();
        products.add(new Composite("@s2", "p4"));
        products.add(new Composite("@s2", "p5"));
        assertEquals(target.calculateComposite(products, f), new BigDecimal("5.0500000000"));

        // Empty products
        initializer.run();
        assertNull(target.calculateComposite(null, f));

        // Unknown Operator
        initializer.run();
        products.add(new Composite("!s1", "p1"));
        assertNull(target.calculateComposite(products, f));

        // Invalid Site 1
        initializer.run();
        products.add(new Composite("s", "p1"));
        assertNull(target.calculateComposite(products, f));

        // Invalid Site 2
        initializer.run();
        products.add(new Composite(null, "p1"));
        assertNull(target.calculateComposite(products, f));

        // Null Price
        initializer.run();
        when(f.apply("s2", "p4")).thenReturn(null);
        assertNull(target.calculateComposite(products, f));

        // Zero Price
        initializer.run();
        when(f.apply("s2", "p4")).thenReturn(new BigDecimal("0.00"));
        assertNull(target.calculateComposite(products, f));

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
        NavigableMap<Instant, BigDecimal> result = target.collapsePrices(trades, interval, fromTime, toTime, false);
        assertEquals(result.size(), 12);
        assertEquals(result.remove(Instant.ofEpochMilli(10990)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(10994)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(10998)), null);
        assertEquals(result.remove(Instant.ofEpochMilli(11002)), new BigDecimal("1002.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11006)), new BigDecimal("1006.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11010)), new BigDecimal("1010.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11014)), new BigDecimal("1014.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11018)), new BigDecimal("1018.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11022)), new BigDecimal("1020.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11026)), new BigDecimal("1020.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11030)), new BigDecimal("1020.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(11034)), new BigDecimal("1020.0000000000"));
        assertEquals(result.size(), 0, result.toString());

        // Sum Price Mode
        result = target.collapsePrices(trades, interval, fromTime, toTime, true);
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
        when(trades.get(6).getTimestamp()).thenReturn(Instant.ofEpochMilli(10986)); // Before
        when(trades.get(7).getTimestamp()).thenReturn(Instant.ofEpochMilli(11035)); // After
        when(trades.get(8).getPrice()).thenReturn(null);
        when(trades.get(9).getSize()).thenReturn(null);
        result = target.collapsePrices(trades, interval, fromTime, toTime, true);
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

        // Include previous interval.
        when(trades.get(6).getTimestamp()).thenReturn(Instant.ofEpochMilli(10987));
        result = target.collapsePrices(trades, interval, fromTime, toTime, true);
        assertEquals(result.size(), 12);
        assertEquals(result.remove(Instant.ofEpochMilli(10990)), new BigDecimal("1007.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(10994)), new BigDecimal("1007.0000000000"));
        assertEquals(result.remove(Instant.ofEpochMilli(10998)), new BigDecimal("1007.0000000000"));
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
        when(trades.get(6).getTimestamp()).thenReturn(null);
        when(trades.get(9).getSize()).thenReturn(BigDecimal.ZERO);
        result = target.collapsePrices(trades, interval, fromTime, toTime, true);
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