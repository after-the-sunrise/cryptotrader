package com.after_sunrise.cryptocurrency.cryptotrader.web;

import com.after_sunrise.cryptocurrency.cryptotrader.Cryptotrader;
import com.after_sunrise.cryptocurrency.cryptotrader.core.CryptotraderImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ONE;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class ResteasyContextListenerTest {

    private ResteasyContextListener target;

    @BeforeMethod
    public void setUp() {
        target = new ResteasyContextListener();
    }

    @Test(timeOut = 5000L)
    public void test() throws Exception {

        Cryptotrader trader = Mockito.mock(Cryptotrader.class);

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

}
