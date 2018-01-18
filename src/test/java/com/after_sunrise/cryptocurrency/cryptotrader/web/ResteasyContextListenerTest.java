package com.after_sunrise.cryptocurrency.cryptotrader.web;

import com.after_sunrise.cryptocurrency.cryptotrader.Cryptotrader;
import com.after_sunrise.cryptocurrency.cryptotrader.core.ConfigurationProvider;
import com.after_sunrise.cryptocurrency.cryptotrader.core.CryptotraderImpl;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader;
import com.after_sunrise.cryptocurrency.cryptotrader.web.ResteasyContextListener.EndpointImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.configuration2.MapConfiguration;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ONE;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class ResteasyContextListenerTest {

    private ResteasyContextListener target;

    private EndpointImpl endpoint;

    private ConfigurationProvider provider;

    private Trader trader;

    @BeforeMethod
    public void setUp() {

        target = new ResteasyContextListener();

        provider = mock(ConfigurationProvider.class);

        trader = mock(Trader.class);

        endpoint = new EndpointImpl(Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ConfigurationProvider.class).toInstance(provider);
                bind(Trader.class).toInstance(trader);
            }
        }));

    }

    @Test(timeOut = 5000L)
    public void test() throws Exception {

        Cryptotrader trader = mock(Cryptotrader.class);

        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        Mockito.doAnswer(i -> {
            latch1.countDown();
            return null;
        }).when(trader).execute();
        Mockito.doAnswer(i -> {
            latch2.countDown();
            return null;
        }).when(trader).shutdown();

        CountDownLatch applicationLatch = new CountDownLatch(1);

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Cryptotrader.class).toInstance(trader);
                bind(CountDownLatch.class).toInstance(applicationLatch);
            }
        });

        // Application launched.
        target.withInjector(injector);

        // Confirm started but not killed.
        assertTrue(latch1.await(INTEGER_ONE, SECONDS));
        assertFalse(latch2.await(INTEGER_ONE, SECONDS));

        // Initiate shutdown.
        applicationLatch.countDown();

        // Confirm Killed.
        assertTrue(latch1.await(INTEGER_ONE, SECONDS));
        assertTrue(latch2.await(INTEGER_ONE, SECONDS));

    }

    @Test
    public void testModule() {

        ResteasyContextListener.Module module = new ResteasyContextListener.Module();

        CryptotraderImpl target = new CryptotraderImpl(Guice.createInjector(module));

        try {

            // Do not call "execute()"

            module.preDestroy();

        } finally {

            target.shutdown();

        }

    }

    @Test
    public void testEndpointImpl_triggerTrader() {

        endpoint.triggerTrader();

        verify(trader).trigger();

    }

    @Test
    public void testEndpointImpl_reloadConfiguration() {

        endpoint.reloadConfiguration();

        verify(provider).clear();

    }

    @Test
    public void testEndpointImpl_getConfiguration() {

        Map<String, String> map = new LinkedHashMap<>();
        map.put("foo", "FOO");
        map.put("bar", "BAR");

        when(provider.get()).thenReturn(new MapConfiguration(map));

        assertNotNull(endpoint.getConfiguration());

    }

    @Test
    public void testEndpointImpl_getLaunchTime() {
        assertNotNull(endpoint.getLaunchTime());
    }

    @Test
    public void testEndpointImpl_getConfigTime() {
        assertNotNull(endpoint.getConfigTime());
    }

}
