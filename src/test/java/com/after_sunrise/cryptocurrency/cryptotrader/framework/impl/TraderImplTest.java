package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Pipeline;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static java.time.Duration.ZERO;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class TraderImplTest {

    private TraderImpl target;

    private TestModule module;

    private Pipeline pipeline;

    @BeforeMethod
    public void setUp() throws Exception {

        module = new TestModule();

        pipeline = module.getMock(Pipeline.class);

        target = new TraderImpl(module.createInjector());

    }

    @Test
    public void testControllable() throws Exception {

        assertFalse(target.isClosed());

        target.trigger();
        assertFalse(target.isClosed());

        target.close();
        assertTrue(target.isClosed());

        target.trigger();
        assertFalse(target.isClosed());
        target.trigger();
        assertFalse(target.isClosed());

        target.close();
        assertTrue(target.isClosed());
        target.close();
        assertTrue(target.isClosed());

    }

    @Test(timeOut = 5000)
    public void testTrade() throws Exception {

        Instant time = Instant.now();
        Duration interval = Duration.ofMillis(10);
        String site = "s";
        String instrument = "i";
        Map<String, Set<String>> targets = singletonMap(site, singleton(instrument));
        when(module.getMock(PropertyManager.class).getNow()).thenReturn(time);
        when(module.getMock(PropertyManager.class).getTradingInterval()).thenReturn(interval);
        when(module.getMock(PropertyManager.class).getTradingTargets()).thenReturn(targets);

        AtomicInteger count = new AtomicInteger(3);

        doAnswer(i -> {

            int c = count.decrementAndGet();

            if (c == 0) {
                target.close();
            }
            return null;

        }).when(pipeline).process(any(), any(), any());

        target.trade();

        verify(pipeline, times(3)).process(time.plus(interval), site, instrument);

    }

    @Test(timeOut = 5000)
    public void testTrade_RuntimeException() throws Exception {

        doThrow(new RuntimeException("test")).when(module.getMock(PropertyManager.class)).getNow();

        target.trade();

    }

    @Test(timeOut = 5000)
    public void testTrade_Interrupted() throws Exception {

        Thread.currentThread().interrupt();

        target.trade();

    }

    @Test
    public void testCalculateTime() throws Exception {

        Instant time = Instant.now();
        Duration interval = Duration.ofMillis(10);
        when(module.getMock(PropertyManager.class).getNow()).thenReturn(time);

        when(module.getMock(PropertyManager.class).getTradingInterval()).thenReturn(interval);
        assertEquals(target.calculateTime(), time.plus(interval));

        when(module.getMock(PropertyManager.class).getTradingInterval()).thenReturn(null);
        assertEquals(target.calculateTime(), time);

    }

    @Test
    public void testCalculateInterval() throws Exception {

        Duration interval = Duration.ofMillis(10);
        Instant t1 = Instant.now();
        Instant t2 = t1.plus(interval);
        when(module.getMock(PropertyManager.class).getNow()).thenReturn(t1);

        assertEquals(target.calculateInterval(t2), interval);
        assertEquals(target.calculateInterval(t1), ZERO);
        assertEquals(target.calculateInterval(t1.minus(interval)), ZERO);
        assertEquals(target.calculateInterval(null), ZERO);

    }

}
