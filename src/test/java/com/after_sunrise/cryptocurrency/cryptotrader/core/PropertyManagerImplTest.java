package com.after_sunrise.cryptocurrency.cryptotrader.core;

import com.google.common.io.Resources;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyType.*;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class PropertyManagerImplTest implements InvocationHandler {

    private PropertyManagerImpl target;

    private Configuration conf;

    private Throwable throwable;

    @BeforeMethod
    public void setUp() throws Exception {

        URL url = Resources.getResource("cryptotrader-default.properties");

        conf = spy(new Configurations().properties(url));

        throwable = null;

        Class[] c = new Class[]{Configuration.class};

        Object o = Proxy.newProxyInstance(getClass().getClassLoader(), c, this);

        target = new PropertyManagerImpl((Configuration) o);

    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if (throwable != null) {
            throw throwable;
        }

        return method.invoke(conf, args);

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
    public void testGetVersion() throws Exception {

        assertEquals(target.getVersion(), "default");

        doReturn("test").when(conf).getString(VERSION.getKey());
        assertEquals(target.getVersion(), "test");

        throwable = new RuntimeException("test");
        assertEquals(target.getVersion(), "");

    }

    @Test
    public void testGetTradingActive() throws Exception {

        assertEquals(target.getTradingActive(), FALSE);

        doReturn(TRUE).when(conf).getBoolean(TRADING_ACTIVE.getKey());
        assertEquals(target.getTradingActive(), TRUE);

        throwable = new RuntimeException("test");
        assertEquals(target.getTradingActive(), FALSE);

    }

    @Test
    public void testGetTradingInterval() throws Exception {

        assertEquals(target.getTradingInterval(), Duration.ofMinutes(1));

        // Specific
        doReturn(MINUTES.toMillis(3)).when(conf).getLong(TRADING_INTERVAL.getKey());
        assertEquals(target.getTradingInterval(), Duration.ofMinutes(3));

        // Floor
        doReturn(Long.MIN_VALUE).when(conf).getLong(TRADING_INTERVAL.getKey());
        assertEquals(target.getTradingInterval(), Duration.ofSeconds(1));

        // Ceiling
        doReturn(Long.MAX_VALUE).when(conf).getLong(TRADING_INTERVAL.getKey());
        assertEquals(target.getTradingInterval(), Duration.ofDays(1));

        // Error
        throwable = new RuntimeException("test");
        assertEquals(target.getTradingInterval(), Duration.ofDays(1));

    }

    @Test
    public void testGetTradingTargets() throws Exception {

        Map<String, Set<String>> targets = target.getTradingTargets();
        assertEquals(targets.size(), 1);
        assertEquals(targets.get("bitflyer").size(), 1);
        assertTrue(targets.get("bitflyer").contains("BTC_JPY"));

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

        throwable = new RuntimeException("test");
        targets = target.getTradingTargets();
        assertTrue(targets.isEmpty());

    }

    @Test
    public void testGetTradingSpread() throws Exception {

        assertEquals(target.getTradingSpread(), new BigDecimal("0.0500"));

        // Specific
        doReturn(new BigDecimal("0.1234")).when(conf).getBigDecimal(TRADING_SPREAD.getKey());
        assertEquals(target.getTradingSpread(), new BigDecimal("0.1234"));

        // Floor
        doReturn(BigDecimal.TEN.negate()).when(conf).getBigDecimal(TRADING_SPREAD.getKey());
        assertEquals(target.getTradingSpread(), ZERO);

        // Ceiling
        doReturn(BigDecimal.TEN).when(conf).getBigDecimal(TRADING_SPREAD.getKey());
        assertEquals(target.getTradingSpread(), ONE);

        // Error
        throwable = new RuntimeException("test");
        assertEquals(target.getTradingSpread(), new BigDecimal("0.0001"));

    }

    @Test
    public void testGetTradingExposure() throws Exception {

        assertEquals(target.getTradingExposure(), new BigDecimal("0.0001"));

        // Specific
        doReturn(new BigDecimal("0.1234")).when(conf).getBigDecimal(TRADING_EXPOSURE.getKey());
        assertEquals(target.getTradingExposure(), new BigDecimal("0.1234"));

        // Floor
        doReturn(BigDecimal.TEN.negate()).when(conf).getBigDecimal(TRADING_EXPOSURE.getKey());
        assertEquals(target.getTradingExposure(), ZERO);

        // Ceiling
        doReturn(BigDecimal.TEN).when(conf).getBigDecimal(TRADING_EXPOSURE.getKey());
        assertEquals(target.getTradingExposure(), ONE);

        // Error
        throwable = new RuntimeException("test");
        assertEquals(target.getTradingExposure(), ZERO);

    }

    @Test
    public void testGetTradingSplit() throws Exception {

        assertEquals(target.getTradingSplit(), ONE);

        // Specific
        doReturn(new BigDecimal("2.3456")).when(conf).getBigDecimal(TRADING_SPLIT.getKey());
        assertEquals(target.getTradingSplit(), ONE.add(ONE));

        // Floor
        doReturn(TEN.negate()).when(conf).getBigDecimal(TRADING_SPLIT.getKey());
        assertEquals(target.getTradingSplit(), ONE);

        // Ceiling
        doReturn(TEN.multiply(TEN)).when(conf).getBigDecimal(TRADING_SPLIT.getKey());
        assertEquals(target.getTradingSplit(), TEN);

        // Error
        throwable = new RuntimeException("test");
        assertEquals(target.getTradingSplit(), ONE);

    }

    @Test
    public void testGetTradingAggressiveness() throws Exception {

        assertEquals(target.getTradingAggressiveness(), ZERO.setScale(4));

        // Specific
        doReturn(new BigDecimal("0.3456")).when(conf).getBigDecimal(TRADING_AGGRESSIVENESS.getKey());
        assertEquals(target.getTradingAggressiveness(), new BigDecimal("0.3456"));

        // Floor
        doReturn(TEN.negate()).when(conf).getBigDecimal(TRADING_AGGRESSIVENESS.getKey());
        assertEquals(target.getTradingAggressiveness(), ZERO);

        // Ceiling
        doReturn(TEN.multiply(TEN)).when(conf).getBigDecimal(TRADING_AGGRESSIVENESS.getKey());
        assertEquals(target.getTradingAggressiveness(), TEN);

        // Error
        throwable = new RuntimeException("test");
        assertEquals(target.getTradingAggressiveness(), ZERO);

    }

}
