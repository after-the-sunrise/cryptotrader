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
    public void testGetTradingInterval() throws Exception {

        // Default
        assertEquals(target.getTradingInterval(), Duration.ofMinutes(1));

        // Mocked
        doReturn(MINUTES.toMillis(3)).when(conf).getLong(TRADING_INTERVAL.getKey());
        assertEquals(target.getTradingInterval(), Duration.ofMinutes(3));

        // Ceiling
        doReturn(Long.MAX_VALUE).when(conf).getLong(TRADING_INTERVAL.getKey());
        assertEquals(target.getTradingInterval(), Duration.ofDays(1));

        // Floor
        doReturn(Long.MIN_VALUE).when(conf).getLong(TRADING_INTERVAL.getKey());
        assertEquals(target.getTradingInterval(), Duration.ofSeconds(1));

        // Error
        doThrow(new RuntimeException("test")).when(conf).getLong(TRADING_INTERVAL.getKey());
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
    public void testGetTradingSpread() throws Exception {

        assertEquals(target.getTradingSpread(site, inst), new BigDecimal("0.0500"));

        // Specific
        doReturn(new BigDecimal("0.1234")).when(conf).getBigDecimal(TRADING_SPREAD.getKey());
        assertEquals(target.getTradingSpread(site, inst), new BigDecimal("0.1234"));

        // Floor
        doReturn(BigDecimal.TEN.negate()).when(conf).getBigDecimal(TRADING_SPREAD.getKey());
        assertEquals(target.getTradingSpread(site, inst), ZERO);

        // Ceiling
        doReturn(BigDecimal.TEN).when(conf).getBigDecimal(TRADING_SPREAD.getKey());
        assertEquals(target.getTradingSpread(site, inst), ONE);

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(TRADING_SPREAD.getKey());
        assertEquals(target.getTradingSpread(site, inst), ZERO);
        reset(conf);

        // Override
        target.setTradingSpread(site, inst, new BigDecimal("0.02"));
        assertEquals(target.getTradingSpread(site, inst), new BigDecimal("0.02"));

        // Clear
        target.setTradingSpread(site, inst, null);
        assertEquals(target.getTradingSpread(site, inst), new BigDecimal("0.0500"));

    }

    @Test
    public void testGetTradingExposure() throws Exception {

        // Default
        assertEquals(target.getTradingExposure(site, inst), new BigDecimal("0.0010"));

        // Mocked
        doReturn(new BigDecimal("0.1234")).when(conf).getBigDecimal(TRADING_EXPOSURE.getKey());
        assertEquals(target.getTradingExposure(site, inst), new BigDecimal("0.1234"));

        // Floor
        doReturn(BigDecimal.TEN.negate()).when(conf).getBigDecimal(TRADING_EXPOSURE.getKey());
        assertEquals(target.getTradingExposure(site, inst), ZERO);

        // Ceiling
        doReturn(BigDecimal.TEN).when(conf).getBigDecimal(TRADING_EXPOSURE.getKey());
        assertEquals(target.getTradingExposure(site, inst), ONE);

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(TRADING_EXPOSURE.getKey());
        assertEquals(target.getTradingExposure(site, inst), ZERO);
        reset(conf);

        // Override
        target.setTradingExposure(site, inst, new BigDecimal("0.02"));
        assertEquals(target.getTradingExposure(site, inst), new BigDecimal("0.02"));

        // Clear
        target.setTradingExposure(site, inst, null);
        assertEquals(target.getTradingExposure(site, inst), new BigDecimal("0.0010"));

    }

    @Test
    public void testGetTradingSplit() throws Exception {

        assertEquals(target.getTradingSplit(site, inst), ONE);

        // Specific
        doReturn(new BigDecimal("2.3456")).when(conf).getBigDecimal(TRADING_SPLIT.getKey());
        assertEquals(target.getTradingSplit(site, inst), ONE.add(ONE));

        // Floor
        doReturn(TEN.negate()).when(conf).getBigDecimal(TRADING_SPLIT.getKey());
        assertEquals(target.getTradingSplit(site, inst), ONE);

        // Ceiling
        doReturn(TEN.multiply(TEN)).when(conf).getBigDecimal(TRADING_SPLIT.getKey());
        assertEquals(target.getTradingSplit(site, inst), TEN);

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(TRADING_SPLIT.getKey());
        assertEquals(target.getTradingSplit(site, inst), ONE);
        reset(conf);

        // Override
        target.setTradingSplit(site, inst, TEN);
        assertEquals(target.getTradingSplit(site, inst), TEN);

        // Clear
        target.setTradingSplit(site, inst, null);
        assertEquals(target.getTradingSplit(site, inst), ONE);

    }

    @Test
    public void testGetFundingOffset() throws Exception {

        assertEquals(target.getFundingOffset(site, inst), ZERO);

        // Specific
        doReturn(new BigDecimal("2.3456")).when(conf).getBigDecimal(FUNDING_OFFSET.getKey());
        assertEquals(target.getFundingOffset(site, inst), new BigDecimal("2.3456"));

        // Error
        doThrow(new RuntimeException("test")).when(conf).getBigDecimal(FUNDING_OFFSET.getKey());
        assertEquals(target.getFundingOffset(site, inst), ZERO);
        reset(conf);

        // Override
        target.setFundingOffset(site, inst, TEN);
        assertEquals(target.getFundingOffset(site, inst), TEN);

        // Clear
        target.setFundingOffset(site, inst, null);
        assertEquals(target.getFundingOffset(site, inst), ZERO);

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

}
