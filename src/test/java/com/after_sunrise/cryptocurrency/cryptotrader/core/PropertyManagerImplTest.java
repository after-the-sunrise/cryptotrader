package com.after_sunrise.cryptocurrency.cryptotrader.core;

import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyType.*;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.*;
import static java.math.BigDecimal.valueOf;
import static java.util.Collections.singleton;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class PropertyManagerImplTest {

    private PropertyManagerImpl target;

    private Configuration conf;

    private String site;

    private String inst;

    @BeforeMethod
    public void setUp() throws Exception {

        URL url = Resources.getResource("cryptotrader-default.properties");

        conf = spy(new Configurations().properties(url));

        target = new PropertyManagerImpl(conf);

        site = "site";

        inst = "inst";

    }

    @Test
    public void testGetNow() throws Exception {

        Instant t1 = Instant.now();

        MILLISECONDS.sleep(10L);

        Instant t2 = target.getNow();

        MILLISECONDS.sleep(10L);

        Instant t3 = Instant.now();

        assertTrue(t1.isBefore(t2));

        assertTrue(t2.isBefore(t3));

    }

    @Test
    public void testSetGet() {

        conf.setProperty(VERSION.getKey(), "v1");
        conf.setProperty(VERSION.getKey() + ".s.i", "v2");

        // Default only
        assertEquals(target.get(VERSION, null, null, Configuration::getString), "v1");
        assertEquals(target.get(VERSION, "s", null, Configuration::getString), "v1");
        assertEquals(target.get(VERSION, null, "i", Configuration::getString), "v1");
        assertEquals(target.get(VERSION, "s", "i", Configuration::getString), "v2");

        // Overwrite default
        target.set(VERSION, null, null, "o1", v -> v);
        assertEquals(target.get(VERSION, null, null, Configuration::getString), "o1");
        assertEquals(target.get(VERSION, "s", null, Configuration::getString), "o1");
        assertEquals(target.get(VERSION, null, "i", Configuration::getString), "o1");
        assertEquals(target.get(VERSION, "s", "i", Configuration::getString), "v2");

        // Overwrite specific
        target.set(VERSION, "s", "i", "o2", v -> v);
        assertEquals(target.get(VERSION, null, null, Configuration::getString), "o1");
        assertEquals(target.get(VERSION, "s", null, Configuration::getString), "o1");
        assertEquals(target.get(VERSION, null, "i", Configuration::getString), "o1");
        assertEquals(target.get(VERSION, "s", "i", Configuration::getString), "o2");

        // Clear default
        target.set(VERSION, null, null, null, v -> v);
        assertEquals(target.get(VERSION, null, null, Configuration::getString), "v1");
        assertEquals(target.get(VERSION, "s", null, Configuration::getString), "v1");
        assertEquals(target.get(VERSION, null, "i", Configuration::getString), "v1");
        assertEquals(target.get(VERSION, "s", "i", Configuration::getString), "o2");

        // Clear specific
        target.set(VERSION, "s", "i", null, v -> v);
        assertEquals(target.get(VERSION, null, null, Configuration::getString), "v1");
        assertEquals(target.get(VERSION, "s", null, Configuration::getString), "v1");
        assertEquals(target.get(VERSION, null, "i", Configuration::getString), "v1");
        assertEquals(target.get(VERSION, "s", "i", Configuration::getString), "v2");

    }

    @Test
    public void testGetVersion() throws Exception {

        assertEquals(target.getVersion(), "default");

        doReturn("test").when(conf).getString(VERSION.getKey());
        assertEquals(target.getVersion(), "test");

        doThrow(new RuntimeException("test")).when(conf).getString(VERSION.getKey());
        assertEquals(target.getVersion(), "");

    }

    @Test
    public void testGetTradingInterval() throws Exception {

        // Default
        assertEquals(target.getTradingInterval(), Duration.ofMinutes(1));

        // Mocked
        doReturn(valueOf(MINUTES.toMillis(3))).when(conf).getBigDecimal(TRADING_INTERVAL.getKey());
        assertEquals(target.getTradingInterval(), Duration.ofMinutes(3));

        // Ceiling
        doReturn(valueOf(Long.MAX_VALUE)).when(conf).getBigDecimal(TRADING_INTERVAL.getKey());
        assertEquals(target.getTradingInterval(), Duration.ofDays(1));

        // Floor
        doReturn(valueOf(Long.MIN_VALUE)).when(conf).getBigDecimal(TRADING_INTERVAL.getKey());
        assertEquals(target.getTradingInterval(), Duration.ofSeconds(1));

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(TRADING_INTERVAL.getKey());
        assertEquals(target.getTradingInterval(), Duration.ofDays(1));
        reset(conf);

        // Override
        target.setTradingInterval(Duration.ofMillis(12345));
        assertEquals(target.getTradingInterval(), Duration.ofMillis(12345));

        // Clear
        target.setTradingInterval(null);
        assertEquals(target.getTradingInterval(), Duration.ofMinutes(1));

    }

    @Test
    public void testGetTradingThreads() throws Exception {

        // Default
        assertEquals(target.getTradingThreads(), (Integer) 1);

        // Mocked
        doReturn(valueOf(8)).when(conf).getBigDecimal(TRADING_THREADS.getKey());
        assertEquals(target.getTradingThreads(), (Integer) 8);

        // Ceiling
        doReturn(valueOf(Integer.MAX_VALUE)).when(conf).getBigDecimal(TRADING_THREADS.getKey());
        assertEquals(target.getTradingThreads(), Integer.valueOf(Byte.MAX_VALUE));

        // Floor
        doReturn(valueOf(Integer.MIN_VALUE)).when(conf).getBigDecimal(TRADING_THREADS.getKey());
        assertEquals(target.getTradingThreads(), (Integer) 1);

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(TRADING_THREADS.getKey());
        assertEquals(target.getTradingThreads(), (Integer) 1);
        reset(conf);

        // Override
        target.setTradingThreads(3);
        assertEquals(target.getTradingThreads(), (Integer) 3);

        // Clear
        target.setTradingThreads(null);
        assertEquals(target.getTradingThreads(), (Integer) 1);

    }

    @Test
    public void testGetTradingTargets() throws Exception {

        // Default
        Map<String, Set<String>> targets = target.getTradingTargets();
        assertEquals(targets.size(), 1);
        assertEquals(targets.get("bitflyer").size(), 1);
        assertTrue(targets.get("bitflyer").contains("BTC_JPY"));

        // Mocked
        String value = "exch1:ccy1|exch1:ccy2|exch2:ccy1||exch2:|:ccy2|exch3:ccy2:test|";
        doReturn(value).when(conf).getString(TRADING_TARGETS.getKey());
        targets = target.getTradingTargets();
        assertEquals(targets.size(), 3);
        assertEquals(targets.get("exch1").size(), 2);
        assertTrue(targets.get("exch1").contains("ccy1"));
        assertTrue(targets.get("exch1").contains("ccy2"));
        assertEquals(targets.get("exch2").size(), 1);
        assertTrue(targets.get("exch2").contains("ccy1"));
        assertEquals(targets.get("exch3").size(), 1);
        assertTrue(targets.get("exch3").contains("ccy2:test"));

        // Error
        doThrow(new RuntimeException("test")).when(conf).getString(TRADING_TARGETS.getKey());
        targets = target.getTradingTargets();
        assertTrue(targets.isEmpty());
        reset(conf);

        // Overwrite
        Map<String, Set<String>> newTargets = new TreeMap<>();
        newTargets.put("s1", Sets.newTreeSet(Arrays.asList("i1", "i2")));
        newTargets.put("s2", Sets.newTreeSet(Arrays.asList("i2", "i3")));
        target.setTradingTargets(newTargets);
        targets = target.getTradingTargets();
        assertEquals(targets.size(), 2);
        assertEquals(targets.get("s1").size(), 2, target.toString());
        assertTrue(targets.get("s1").contains("i2"));
        assertTrue(targets.get("s1").contains("i2"));
        assertEquals(targets.get("s2").size(), 2);
        assertTrue(targets.get("s2").contains("i2"));
        assertTrue(targets.get("s2").contains("i3"));

        // Clear
        target.setTradingTargets(null);
        targets = target.getTradingTargets();
        assertEquals(targets.size(), 1);
        assertEquals(targets.get("bitflyer").size(), 1);
        assertTrue(targets.get("bitflyer").contains("BTC_JPY"));

    }

    @Test
    public void testGetTradingActive() throws Exception {

        // Default
        assertEquals(target.getTradingActive(site, inst), FALSE);

        // Mocked
        doReturn(TRUE).when(conf).getBoolean(TRADING_ACTIVE.getKey());
        assertEquals(target.getTradingActive(site, inst), TRUE);

        // Mocked Error
        doThrow(new RuntimeException("test")).when(conf).getBoolean(TRADING_ACTIVE.getKey());
        assertEquals(target.getTradingActive(site, inst), FALSE);
        reset(conf);

        // Override
        target.setTradingActive(site, inst, true);
        assertEquals(target.getTradingActive(site, inst), TRUE);

        // Clear
        target.setTradingActive(site, inst, null);
        assertEquals(target.getTradingActive(site, inst), FALSE);

    }

    @Test
    public void testGetTradingFrequency() throws Exception {

        assertEquals(target.getTradingFrequency(site, inst), (Integer) 1);

        // Specific
        doReturn(valueOf(8)).when(conf).getBigDecimal(TRADING_FREQUENCY.getKey());
        assertEquals(target.getTradingFrequency(site, inst), (Integer) 8);

        // Ceiling
        doReturn(valueOf(Integer.MAX_VALUE)).when(conf).getBigDecimal(TRADING_FREQUENCY.getKey());
        assertEquals(target.getTradingFrequency(site, inst), (Integer) Integer.MAX_VALUE);

        // Floor
        doReturn(valueOf(Integer.MIN_VALUE)).when(conf).getBigDecimal(TRADING_FREQUENCY.getKey());
        assertEquals(target.getTradingFrequency(site, inst), (Integer) 1);

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(TRADING_FREQUENCY.getKey());
        assertEquals(target.getTradingFrequency(site, inst), (Integer) 1);
        reset(conf);

        // Override
        target.setTradingFrequency(site, inst, 3);
        assertEquals(target.getTradingFrequency(site, inst), (Integer) 3);

        // Clear
        target.setTradingFrequency(site, inst, null);
        assertEquals(target.getTradingFrequency(site, inst), (Integer) 1);

    }

    @Test
    public void testGetTradingSpread() throws Exception {

        assertEquals(target.getTradingSpread(site, inst), new BigDecimal("0.0100"));

        // Specific
        doReturn(new BigDecimal("0.1234")).when(conf).getBigDecimal(TRADING_SPREAD.getKey());
        assertEquals(target.getTradingSpread(site, inst), new BigDecimal("0.1234"));

        // Ceiling
        doReturn(valueOf(Integer.MAX_VALUE)).when(conf).getBigDecimal(TRADING_SPREAD.getKey());
        assertEquals(target.getTradingSpread(site, inst), ONE);

        // Floor
        doReturn(valueOf(Integer.MIN_VALUE)).when(conf).getBigDecimal(TRADING_SPREAD.getKey());
        assertEquals(target.getTradingSpread(site, inst), ZERO);

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(TRADING_SPREAD.getKey());
        assertEquals(target.getTradingSpread(site, inst), ZERO);
        reset(conf);

        // Override
        target.setTradingSpread(site, inst, new BigDecimal("0.02"));
        assertEquals(target.getTradingSpread(site, inst), new BigDecimal("0.02"));

        // Clear
        target.setTradingSpread(site, inst, null);
        assertEquals(target.getTradingSpread(site, inst), new BigDecimal("0.0100"));

    }

    @Test
    public void testGetTradingSpreadAsk() throws Exception {

        assertEquals(target.getTradingSpreadAsk(site, inst), new BigDecimal("0.0000"));

        // Specific
        doReturn(new BigDecimal("0.1234")).when(conf).getBigDecimal(TRADING_SPREAD_ASK.getKey());
        assertEquals(target.getTradingSpreadAsk(site, inst), new BigDecimal("0.1234"));

        // Ceiling
        doReturn(valueOf(Integer.MAX_VALUE)).when(conf).getBigDecimal(TRADING_SPREAD_ASK.getKey());
        assertEquals(target.getTradingSpreadAsk(site, inst), ONE);

        // Floor
        doReturn(valueOf(Integer.MIN_VALUE)).when(conf).getBigDecimal(TRADING_SPREAD_ASK.getKey());
        assertEquals(target.getTradingSpreadAsk(site, inst), ONE.negate());

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(TRADING_SPREAD_ASK.getKey());
        assertEquals(target.getTradingSpreadAsk(site, inst), ZERO);
        reset(conf);

        // Override
        target.setTradingSpreadAsk(site, inst, new BigDecimal("0.02"));
        assertEquals(target.getTradingSpreadAsk(site, inst), new BigDecimal("0.02"));

        // Clear
        target.setTradingSpreadAsk(site, inst, null);
        assertEquals(target.getTradingSpreadAsk(site, inst), new BigDecimal("0.0000"));

    }

    @Test
    public void testGetTradingSpreadBid() throws Exception {

        assertEquals(target.getTradingSpreadBid(site, inst), new BigDecimal("0.0000"));

        // Specific
        doReturn(new BigDecimal("0.1234")).when(conf).getBigDecimal(TRADING_SPREAD_BID.getKey());
        assertEquals(target.getTradingSpreadBid(site, inst), new BigDecimal("0.1234"));

        // Ceiling
        doReturn(valueOf(Integer.MAX_VALUE)).when(conf).getBigDecimal(TRADING_SPREAD_BID.getKey());
        assertEquals(target.getTradingSpreadBid(site, inst), ONE);

        // Floor
        doReturn(valueOf(Integer.MIN_VALUE)).when(conf).getBigDecimal(TRADING_SPREAD_BID.getKey());
        assertEquals(target.getTradingSpreadBid(site, inst), ONE.negate());

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(TRADING_SPREAD_BID.getKey());
        assertEquals(target.getTradingSpreadBid(site, inst), ZERO);
        reset(conf);

        // Override
        target.setTradingSpreadBid(site, inst, new BigDecimal("0.02"));
        assertEquals(target.getTradingSpreadBid(site, inst), new BigDecimal("0.02"));

        // Clear
        target.setTradingSpreadBid(site, inst, null);
        assertEquals(target.getTradingSpreadBid(site, inst), new BigDecimal("0.0000"));

    }

    @Test
    public void testGetTradingSigma() throws Exception {

        // Default
        assertEquals(target.getTradingSigma(site, inst), new BigDecimal("0.00"));

        // Mocked
        doReturn(new BigDecimal("0.12")).when(conf).getBigDecimal(TRADING_SIGMA.getKey());
        assertEquals(target.getTradingSigma(site, inst), new BigDecimal("0.12"));

        // Ceiling
        doReturn(valueOf(Integer.MAX_VALUE)).when(conf).getBigDecimal(TRADING_SIGMA.getKey());
        assertEquals(target.getTradingSigma(site, inst), valueOf(Integer.MAX_VALUE));

        // Floor
        doReturn(valueOf(Integer.MIN_VALUE)).when(conf).getBigDecimal(TRADING_SIGMA.getKey());
        assertEquals(target.getTradingSigma(site, inst), ZERO);

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(TRADING_SIGMA.getKey());
        assertEquals(target.getTradingSigma(site, inst), ZERO);
        reset(conf);

        // Override
        target.setTradingSigma(site, inst, new BigDecimal("3.00"));
        assertEquals(target.getTradingSigma(site, inst), new BigDecimal("3.00"));

        // Clear
        target.setTradingSigma(site, inst, null);
        assertEquals(target.getTradingSigma(site, inst), new BigDecimal("0.00"));

    }

    @Test
    public void testGetTradingSamples() throws Exception {

        assertEquals(target.getTradingSamples(site, inst), (Integer) 0);

        // Specific
        doReturn(valueOf(8)).when(conf).getBigDecimal(TRADING_SAMPLES.getKey());
        assertEquals(target.getTradingSamples(site, inst), (Integer) 8);

        // Ceiling
        doReturn(valueOf(Integer.MAX_VALUE)).when(conf).getBigDecimal(TRADING_SAMPLES.getKey());
        assertEquals(target.getTradingSamples(site, inst), (Integer) Integer.MAX_VALUE);

        // Floor
        doReturn(valueOf(Integer.MIN_VALUE)).when(conf).getBigDecimal(TRADING_SAMPLES.getKey());
        assertEquals(target.getTradingSamples(site, inst), (Integer) 0);

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(TRADING_SAMPLES.getKey());
        assertEquals(target.getTradingSamples(site, inst), (Integer) 0);
        reset(conf);

        // Override
        target.setTradingSamples(site, inst, 3);
        assertEquals(target.getTradingSamples(site, inst), (Integer) 3);

        // Clear
        target.setTradingSamples(site, inst, null);
        assertEquals(target.getTradingSamples(site, inst), (Integer) 0);

    }

    @Test
    public void testGetTradingExposure() throws Exception {

        // Default
        assertEquals(target.getTradingExposure(site, inst), new BigDecimal("0.0000"));

        // Mocked
        doReturn(new BigDecimal("0.1234")).when(conf).getBigDecimal(TRADING_EXPOSURE.getKey());
        assertEquals(target.getTradingExposure(site, inst), new BigDecimal("0.1234"));

        // Ceiling
        doReturn(valueOf(Integer.MAX_VALUE)).when(conf).getBigDecimal(TRADING_EXPOSURE.getKey());
        assertEquals(target.getTradingExposure(site, inst), ONE);

        // Floor
        doReturn(valueOf(Integer.MIN_VALUE)).when(conf).getBigDecimal(TRADING_EXPOSURE.getKey());
        assertEquals(target.getTradingExposure(site, inst), ZERO);

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(TRADING_EXPOSURE.getKey());
        assertEquals(target.getTradingExposure(site, inst), ZERO);
        reset(conf);

        // Override
        target.setTradingExposure(site, inst, new BigDecimal("0.02"));
        assertEquals(target.getTradingExposure(site, inst), new BigDecimal("0.02"));

        // Clear
        target.setTradingExposure(site, inst, null);
        assertEquals(target.getTradingExposure(site, inst), new BigDecimal("0.0000"));

    }

    @Test
    public void testGetTradingThreshold() throws Exception {

        // Default
        assertEquals(target.getTradingThreshold(site, inst), new BigDecimal("0.00000000"));

        // Mocked
        doReturn(new BigDecimal("0.1234")).when(conf).getBigDecimal(TRADING_THRESHOLD.getKey());
        assertEquals(target.getTradingThreshold(site, inst), new BigDecimal("0.1234"));

        // Ceiling
        doReturn(valueOf(Integer.MAX_VALUE)).when(conf).getBigDecimal(TRADING_THRESHOLD.getKey());
        assertEquals(target.getTradingThreshold(site, inst), valueOf(Integer.MAX_VALUE));

        // Floor
        doReturn(valueOf(Integer.MIN_VALUE)).when(conf).getBigDecimal(TRADING_THRESHOLD.getKey());
        assertEquals(target.getTradingThreshold(site, inst), ZERO);

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(TRADING_THRESHOLD.getKey());
        assertEquals(target.getTradingThreshold(site, inst), ZERO);
        reset(conf);

        // Override
        target.setTradingThreshold(site, inst, new BigDecimal("0.02"));
        assertEquals(target.getTradingThreshold(site, inst), new BigDecimal("0.02"));

        // Clear
        target.setTradingThreshold(site, inst, null);
        assertEquals(target.getTradingThreshold(site, inst), new BigDecimal("0.00000000"));

    }

    @Test
    public void testGetTradingMinimum() throws Exception {

        // Default
        assertEquals(target.getTradingMinimum(site, inst), new BigDecimal("0.00000000"));

        // Mocked
        doReturn(new BigDecimal("0.1234")).when(conf).getBigDecimal(TRADING_MINIMUM.getKey());
        assertEquals(target.getTradingMinimum(site, inst), new BigDecimal("0.1234"));

        // Ceiling
        doReturn(valueOf(Integer.MAX_VALUE)).when(conf).getBigDecimal(TRADING_MINIMUM.getKey());
        assertEquals(target.getTradingMinimum(site, inst), valueOf(Integer.MAX_VALUE));

        // Floor
        doReturn(valueOf(Integer.MIN_VALUE)).when(conf).getBigDecimal(TRADING_MINIMUM.getKey());
        assertEquals(target.getTradingMinimum(site, inst), ZERO);

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(TRADING_MINIMUM.getKey());
        assertEquals(target.getTradingMinimum(site, inst), ZERO);
        reset(conf);

        // Override
        target.setTradingMinimum(site, inst, new BigDecimal("0.02"));
        assertEquals(target.getTradingMinimum(site, inst), new BigDecimal("0.02"));

        // Clear
        target.setTradingMinimum(site, inst, null);
        assertEquals(target.getTradingMinimum(site, inst), new BigDecimal("0.00000000"));

    }

    @Test
    public void testGetTradingAversion() throws Exception {

        // Default
        assertEquals(target.getTradingAversion(site, inst), new BigDecimal("0.0"));

        // Mocked
        doReturn(new BigDecimal("0.5")).when(conf).getBigDecimal(TRADING_AVERSION.getKey());
        assertEquals(target.getTradingAversion(site, inst), new BigDecimal("0.5"));

        // Ceiling
        doReturn(valueOf(Integer.MAX_VALUE)).when(conf).getBigDecimal(TRADING_AVERSION.getKey());
        assertEquals(target.getTradingAversion(site, inst), valueOf(Integer.MAX_VALUE));

        // Floor
        doReturn(valueOf(Integer.MIN_VALUE)).when(conf).getBigDecimal(TRADING_AVERSION.getKey());
        assertEquals(target.getTradingAversion(site, inst), ZERO);

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(TRADING_AVERSION.getKey());
        assertEquals(target.getTradingAversion(site, inst), ONE);
        reset(conf);

        // Override
        target.setTradingAversion(site, inst, new BigDecimal("2.5"));
        assertEquals(target.getTradingAversion(site, inst), new BigDecimal("2.5"));

        // Clear
        target.setTradingAversion(site, inst, null);
        assertEquals(target.getTradingAversion(site, inst), new BigDecimal("0.0"));

    }

    @Test
    public void testGetTradingSplit() throws Exception {

        assertEquals(target.getTradingSplit(site, inst), (Integer) 1);

        // Specific
        doReturn(new BigDecimal("2.3456")).when(conf).getBigDecimal(TRADING_SPLIT.getKey());
        assertEquals(target.getTradingSplit(site, inst), (Integer) 2);

        // Ceiling
        doReturn(valueOf(Integer.MAX_VALUE)).when(conf).getBigDecimal(TRADING_SPLIT.getKey());
        assertEquals(target.getTradingSplit(site, inst), (Integer) 10);

        // Floor
        doReturn(valueOf(Integer.MIN_VALUE)).when(conf).getBigDecimal(TRADING_SPLIT.getKey());
        assertEquals(target.getTradingSplit(site, inst), (Integer) 1);

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(TRADING_SPLIT.getKey());
        assertEquals(target.getTradingSplit(site, inst), (Integer) 1);
        reset(conf);

        // Override
        target.setTradingSplit(site, inst, 10);
        assertEquals(target.getTradingSplit(site, inst), (Integer) 10);

        // Clear
        target.setTradingSplit(site, inst, null);
        assertEquals(target.getTradingSplit(site, inst), (Integer) 1);

    }

    @Test
    public void testGetTradingDuration() throws Exception {

        assertEquals(target.getTradingDuration(site, inst), Duration.ofMillis(0));

        // Specific
        doReturn(valueOf(300000L)).when(conf).getBigDecimal(TRADING_DURATION.getKey());
        assertEquals(target.getTradingDuration(site, inst), Duration.ofMillis(300000));

        // Ceiling
        doReturn(valueOf(Long.MAX_VALUE)).when(conf).getBigDecimal(TRADING_DURATION.getKey());
        assertEquals(target.getTradingDuration(site, inst), Duration.ofMillis(Long.MAX_VALUE));

        // Floor
        doReturn(valueOf(Long.MIN_VALUE)).when(conf).getBigDecimal(TRADING_DURATION.getKey());
        assertEquals(target.getTradingDuration(site, inst), Duration.ofMillis(Long.MIN_VALUE));

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(TRADING_DURATION.getKey());
        assertEquals(target.getTradingDuration(site, inst), Duration.ZERO);
        reset(conf);

        // Override
        target.setTradingDuration(site, inst, Duration.ofMillis(1));
        assertEquals(target.getTradingDuration(site, inst), Duration.ofMillis(1));

        // Clear
        target.setTradingDuration(site, inst, null);
        assertEquals(target.getTradingDuration(site, inst), Duration.ofMillis(0));

    }

    @Test
    public void testGetFundingOffset() throws Exception {

        assertEquals(target.getFundingOffset(site, inst), new BigDecimal("0.00"));

        // Specific
        doReturn(new BigDecimal("2.3456")).when(conf).getBigDecimal(FUNDING_OFFSET.getKey());
        assertEquals(target.getFundingOffset(site, inst), new BigDecimal("2.3456"));

        // Ceiling
        doReturn(valueOf(Integer.MAX_VALUE)).when(conf).getBigDecimal(FUNDING_OFFSET.getKey());
        assertEquals(target.getFundingOffset(site, inst), valueOf(Integer.MAX_VALUE));

        // Floor
        doReturn(valueOf(Integer.MIN_VALUE)).when(conf).getBigDecimal(FUNDING_OFFSET.getKey());
        assertEquals(target.getFundingOffset(site, inst), valueOf(Integer.MIN_VALUE));

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(FUNDING_OFFSET.getKey());
        assertEquals(target.getFundingOffset(site, inst), ZERO);
        reset(conf);

        // Override
        target.setFundingOffset(site, inst, TEN);
        assertEquals(target.getFundingOffset(site, inst), TEN);

        // Clear
        target.setFundingOffset(site, inst, null);
        assertEquals(target.getFundingOffset(site, inst), new BigDecimal("0.00"));

    }

    @Test
    public void testGetFundingMultiplierProducts() throws Exception {

        // Default
        Map<String, Set<String>> targets = target.getFundingMultiplierProducts(site, inst);
        assertEquals(targets.size(), 0);

        // Mocked
        String value = "exch1:ccy1|exch1:ccy2|exch2:ccy1||exch2:|:ccy2|exch3:ccy2:test|";
        doReturn(value).when(conf).getString(FUNDING_MULTIPLIER_PRODUCTS.getKey());
        targets = target.getFundingMultiplierProducts(site, inst);
        assertEquals(targets.size(), 3);
        assertEquals(targets.get("exch1").size(), 2);
        assertTrue(targets.get("exch1").contains("ccy1"));
        assertTrue(targets.get("exch1").contains("ccy2"));
        assertEquals(targets.get("exch2").size(), 1);
        assertTrue(targets.get("exch2").contains("ccy1"));
        assertEquals(targets.get("exch3").size(), 1);
        assertTrue(targets.get("exch3").contains("ccy2:test"));

        // Error
        doThrow(new RuntimeException("test")).when(conf).getString(FUNDING_MULTIPLIER_PRODUCTS.getKey());
        targets = target.getFundingMultiplierProducts(site, inst);
        assertTrue(targets.isEmpty());
        reset(conf);

        // Overwrite
        Map<String, Set<String>> newTargets = new TreeMap<>();
        newTargets.put("s1", Sets.newTreeSet(Arrays.asList("i1", "i2")));
        newTargets.put("s2", Sets.newTreeSet(Arrays.asList("i2", "i3")));
        target.setFundingMultiplierProducts(site, inst, newTargets);
        targets = target.getFundingMultiplierProducts(site, inst);
        assertEquals(targets.size(), 2);
        assertEquals(targets.get("s1").size(), 2, target.toString());
        assertTrue(targets.get("s1").contains("i2"));
        assertTrue(targets.get("s1").contains("i2"));
        assertEquals(targets.get("s2").size(), 2);
        assertTrue(targets.get("s2").contains("i2"));
        assertTrue(targets.get("s2").contains("i3"));

        // Clear
        target.setFundingMultiplierProducts(site, inst, null);
        targets = target.getHedgeProducts(site, inst);
        assertEquals(targets.size(), 0);

    }

    @Test
    public void testGetFundingPositiveMultiplier() throws Exception {

        assertEquals(target.getFundingPositiveMultiplier(site, inst), new BigDecimal("1.0"));

        // Specific
        doReturn(new BigDecimal("2.3456")).when(conf).getBigDecimal(FUNDING_POSITIVE_MULTIPLIER.getKey());
        assertEquals(target.getFundingPositiveMultiplier(site, inst), new BigDecimal("2.3456"));

        // Ceiling
        doReturn(valueOf(Integer.MAX_VALUE)).when(conf).getBigDecimal(FUNDING_POSITIVE_MULTIPLIER.getKey());
        assertEquals(target.getFundingPositiveMultiplier(site, inst), valueOf(Integer.MAX_VALUE));

        // Floor
        doReturn(valueOf(Integer.MIN_VALUE)).when(conf).getBigDecimal(FUNDING_POSITIVE_MULTIPLIER.getKey());
        assertEquals(target.getFundingPositiveMultiplier(site, inst), valueOf(Integer.MIN_VALUE));

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(FUNDING_POSITIVE_MULTIPLIER.getKey());
        assertEquals(target.getFundingPositiveMultiplier(site, inst), ONE);
        reset(conf);

        // Override
        target.setFundingPositiveMultiplier(site, inst, TEN);
        assertEquals(target.getFundingPositiveMultiplier(site, inst), TEN);

        // Clear
        target.setFundingPositiveMultiplier(site, inst, null);
        assertEquals(target.getFundingPositiveMultiplier(site, inst), new BigDecimal("1.0"));

    }

    @Test
    public void testGetFundingNegativeMultiplier() throws Exception {

        assertEquals(target.getFundingNegativeMultiplier(site, inst), new BigDecimal("1.0"));

        // Specific
        doReturn(new BigDecimal("2.3456")).when(conf).getBigDecimal(FUNDING_NEGATIVE_MULTIPLIER.getKey());
        assertEquals(target.getFundingNegativeMultiplier(site, inst), new BigDecimal("2.3456"));

        // Ceiling
        doReturn(valueOf(Integer.MAX_VALUE)).when(conf).getBigDecimal(FUNDING_NEGATIVE_MULTIPLIER.getKey());
        assertEquals(target.getFundingNegativeMultiplier(site, inst), valueOf(Integer.MAX_VALUE));

        // Floor
        doReturn(valueOf(Integer.MIN_VALUE)).when(conf).getBigDecimal(FUNDING_NEGATIVE_MULTIPLIER.getKey());
        assertEquals(target.getFundingNegativeMultiplier(site, inst), valueOf(Integer.MIN_VALUE));

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(FUNDING_NEGATIVE_MULTIPLIER.getKey());
        assertEquals(target.getFundingNegativeMultiplier(site, inst), ONE);
        reset(conf);

        // Override
        target.setFundingNegativeMultiplier(site, inst, TEN);
        assertEquals(target.getFundingNegativeMultiplier(site, inst), TEN);

        // Clear
        target.setFundingNegativeMultiplier(site, inst, null);
        assertEquals(target.getFundingNegativeMultiplier(site, inst), new BigDecimal("1.0"));

    }

    @Test
    public void testGetFundingPositiveThreshold() throws Exception {

        assertEquals(target.getFundingPositiveThreshold(site, inst), new BigDecimal("0.0"));

        // Specific
        doReturn(new BigDecimal("2.3456")).when(conf).getBigDecimal(FUNDING_POSITIVE_THRESHOLD.getKey());
        assertEquals(target.getFundingPositiveThreshold(site, inst), new BigDecimal("2.3456"));

        // Ceiling
        doReturn(valueOf(Integer.MAX_VALUE)).when(conf).getBigDecimal(FUNDING_POSITIVE_THRESHOLD.getKey());
        assertEquals(target.getFundingPositiveThreshold(site, inst), valueOf(Integer.MAX_VALUE));

        // Floor
        doReturn(valueOf(Integer.MIN_VALUE)).when(conf).getBigDecimal(FUNDING_POSITIVE_THRESHOLD.getKey());
        assertEquals(target.getFundingPositiveThreshold(site, inst), valueOf(Integer.MIN_VALUE));

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(FUNDING_POSITIVE_THRESHOLD.getKey());
        assertEquals(target.getFundingPositiveThreshold(site, inst), ONE);
        reset(conf);

        // Override
        target.setFundingPositiveThreshold(site, inst, TEN);
        assertEquals(target.getFundingPositiveThreshold(site, inst), TEN);

        // Clear
        target.setFundingPositiveThreshold(site, inst, null);
        assertEquals(target.getFundingPositiveThreshold(site, inst), new BigDecimal("0.0"));

    }

    @Test
    public void testGetFundingNegativeThreshold() throws Exception {

        assertEquals(target.getFundingNegativeThreshold(site, inst), new BigDecimal("0.0"));

        // Specific
        doReturn(new BigDecimal("2.3456")).when(conf).getBigDecimal(FUNDING_NEGATIVE_THRESHOLD.getKey());
        assertEquals(target.getFundingNegativeThreshold(site, inst), new BigDecimal("2.3456"));

        // Ceiling
        doReturn(valueOf(Integer.MAX_VALUE)).when(conf).getBigDecimal(FUNDING_NEGATIVE_THRESHOLD.getKey());
        assertEquals(target.getFundingNegativeThreshold(site, inst), valueOf(Integer.MAX_VALUE));

        // Floor
        doReturn(valueOf(Integer.MIN_VALUE)).when(conf).getBigDecimal(FUNDING_NEGATIVE_THRESHOLD.getKey());
        assertEquals(target.getFundingNegativeThreshold(site, inst), valueOf(Integer.MIN_VALUE));

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(FUNDING_NEGATIVE_THRESHOLD.getKey());
        assertEquals(target.getFundingNegativeThreshold(site, inst), ONE);
        reset(conf);

        // Override
        target.setFundingNegativeThreshold(site, inst, TEN);
        assertEquals(target.getFundingNegativeThreshold(site, inst), TEN);

        // Clear
        target.setFundingNegativeThreshold(site, inst, null);
        assertEquals(target.getFundingNegativeThreshold(site, inst), new BigDecimal("0.0"));

    }

    @Test
    public void testGetHedgeProducts() throws Exception {

        // Default
        Map<String, Set<String>> targets = target.getHedgeProducts(site, inst);
        assertEquals(targets.size(), 0);

        // Mocked
        String value = "exch1:ccy1|exch1:ccy2|exch2:ccy1||exch2:|:ccy2|exch3:ccy2:test|";
        doReturn(value).when(conf).getString(HEDGE_PRODUCTS.getKey());
        targets = target.getHedgeProducts(site, inst);
        assertEquals(targets.size(), 3);
        assertEquals(targets.get("exch1").size(), 2);
        assertTrue(targets.get("exch1").contains("ccy1"));
        assertTrue(targets.get("exch1").contains("ccy2"));
        assertEquals(targets.get("exch2").size(), 1);
        assertTrue(targets.get("exch2").contains("ccy1"));
        assertEquals(targets.get("exch3").size(), 1);
        assertTrue(targets.get("exch3").contains("ccy2:test"));

        // Error
        doThrow(new RuntimeException("test")).when(conf).getString(HEDGE_PRODUCTS.getKey());
        targets = target.getHedgeProducts(site, inst);
        assertTrue(targets.isEmpty());
        reset(conf);

        // Overwrite
        Map<String, Set<String>> newTargets = new TreeMap<>();
        newTargets.put("s1", Sets.newTreeSet(Arrays.asList("i1", "i2")));
        newTargets.put("s2", Sets.newTreeSet(Arrays.asList("i2", "i3")));
        target.setHedgeProducts(site, inst, newTargets);
        targets = target.getHedgeProducts(site, inst);
        assertEquals(targets.size(), 2);
        assertEquals(targets.get("s1").size(), 2, target.toString());
        assertTrue(targets.get("s1").contains("i2"));
        assertTrue(targets.get("s1").contains("i2"));
        assertEquals(targets.get("s2").size(), 2);
        assertTrue(targets.get("s2").contains("i2"));
        assertTrue(targets.get("s2").contains("i3"));

        // Clear
        target.setHedgeProducts(site, inst, null);
        targets = target.getHedgeProducts(site, inst);
        assertEquals(targets.size(), 0);

    }

    @Test
    public void testGetEstimators() throws Exception {

        assertEquals(target.getEstimators(site, inst), singleton("NullEstimator"));

        // Specific
        doReturn("Test1||Test2||").when(conf).getString(ESTIMATORS.getKey());
        assertEquals(target.getEstimators(site, inst), Sets.newHashSet("Test1", "Test2"));

        // Error
        doThrow(new RuntimeException("test")).when(conf).getString(ESTIMATORS.getKey());
        assertEquals(target.getEstimators(site, inst).size(), 0);
        reset(conf);

        // Override
        target.setEstimators(site, inst, singleton("MyEstimator"));
        assertEquals(target.getEstimators(site, inst), singleton("MyEstimator"));

        // Clear
        target.setEstimators(site, inst, null);
        assertEquals(target.getEstimators(site, inst), singleton("NullEstimator"));

    }

    @Test
    public void testGetEstimatorComposites() throws Exception {

        // Default
        Map<String, Set<String>> targets = target.getEstimatorComposites(site, inst);
        assertEquals(targets.size(), 0);

        // Mocked
        String value = "exch1:ccy1|exch1:ccy2|exch2:ccy1||exch2:|:ccy2|exch3:ccy2:test|";
        doReturn(value).when(conf).getString(ESTIMATOR_COMPOSITES.getKey());
        targets = target.getEstimatorComposites(site, inst);
        assertEquals(targets.size(), 3);
        assertEquals(targets.get("exch1").size(), 2);
        assertTrue(targets.get("exch1").contains("ccy1"));
        assertTrue(targets.get("exch1").contains("ccy2"));
        assertEquals(targets.get("exch2").size(), 1);
        assertTrue(targets.get("exch2").contains("ccy1"));
        assertEquals(targets.get("exch3").size(), 1);
        assertTrue(targets.get("exch3").contains("ccy2:test"));

        // Error
        doThrow(new RuntimeException("test")).when(conf).getString(ESTIMATOR_COMPOSITES.getKey());
        targets = target.getEstimatorComposites(site, inst);
        assertTrue(targets.isEmpty());
        reset(conf);

        // Overwrite
        Map<String, Set<String>> newTargets = new TreeMap<>();
        newTargets.put("s1", Sets.newTreeSet(Arrays.asList("i1", "i2")));
        newTargets.put("s2", Sets.newTreeSet(Arrays.asList("i2", "i3")));
        target.setEstimatorComposites(site, inst, newTargets);
        targets = target.getEstimatorComposites(site, inst);
        assertEquals(targets.size(), 2);
        assertEquals(targets.get("s1").size(), 2, target.toString());
        assertTrue(targets.get("s1").contains("i2"));
        assertTrue(targets.get("s1").contains("i2"));
        assertEquals(targets.get("s2").size(), 2);
        assertTrue(targets.get("s2").contains("i2"));
        assertTrue(targets.get("s2").contains("i3"));

        // Clear
        target.setEstimatorComposites(site, inst, null);
        targets = target.getEstimatorComposites(site, inst);
        assertEquals(targets.size(), 0);

    }

    @Test
    public void testGetEstimationThreshold() throws Exception {

        assertEquals(target.getEstimationThreshold(site, inst), new BigDecimal("0.00"));

        // Specific
        doReturn(new BigDecimal("0.3456")).when(conf).getBigDecimal(ESTIMATION_THRESHOLD.getKey());
        assertEquals(target.getEstimationThreshold(site, inst), new BigDecimal("0.3456"));

        // Ceiling
        doReturn(valueOf(Integer.MAX_VALUE)).when(conf).getBigDecimal(ESTIMATION_THRESHOLD.getKey());
        assertEquals(target.getEstimationThreshold(site, inst), ONE);

        // Floor
        doReturn(valueOf(Integer.MIN_VALUE)).when(conf).getBigDecimal(ESTIMATION_THRESHOLD.getKey());
        assertEquals(target.getEstimationThreshold(site, inst), ZERO);

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(ESTIMATION_THRESHOLD.getKey());
        assertEquals(target.getEstimationThreshold(site, inst), ZERO);
        reset(conf);

        // Override
        target.setEstimationThreshold(site, inst, new BigDecimal("0.12"));
        assertEquals(target.getEstimationThreshold(site, inst), new BigDecimal("0.12"));

        // Clear
        target.setEstimationThreshold(site, inst, null);
        assertEquals(target.getEstimationThreshold(site, inst), new BigDecimal("0.00"));

    }

}
