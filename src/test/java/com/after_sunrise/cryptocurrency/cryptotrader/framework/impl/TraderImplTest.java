package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.core.Composite;
import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Pipeline;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

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

        target = spy(new TraderImpl(module.createInjector()));

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

        Instant now = Instant.now();
        when(module.getMock(PropertyManager.class).getNow()).thenReturn(now, now.plusMillis(123));

        Duration interval = Duration.ofMillis(50);
        when(module.getMock(PropertyManager.class).getTradingInterval()).thenReturn(interval);

        AtomicInteger count = new AtomicInteger(3);

        doAnswer(i -> {

            int c = count.decrementAndGet();

            if (c == 0) {
                target.close();
            }

            return null;

        }).when(target).processPipeline(any());

        target.trade();

        verify(target, times(3)).processPipeline(any());

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

    @Test(timeOut = 5000)
    public void testProcessPipeline() throws InterruptedException {

        Instant now = Instant.now();
        String site = "s";
        String instrument = "i";

        List<Composite> targets = singletonList(new Composite(site, instrument));
        when(module.getMock(PropertyManager.class).getTradingTargets()).thenReturn(targets);
        when(module.getMock(PropertyManager.class).getTradingInterval()).thenReturn(Duration.ofMillis(123));
        when(module.getMock(PropertyManager.class).getTradingFrequency(site, instrument)).thenReturn(3);
        when(module.getMock(PropertyManager.class).getTradingSeed(site, instrument)).thenReturn(0);

        doNothing().when(module.getMock(Pipeline.class)).process(any(), any(), any(), any());

        // 0
        target.processPipeline(now);
        verify(pipeline, times(1)).process(now, now.plusMillis(123 * 3), site, instrument);

        // 1, 2
        target.processPipeline(now);
        target.processPipeline(now);
        verifyNoMoreInteractions(pipeline);

        // 3
        target.processPipeline(now);
        verify(pipeline, times(2)).process(now, now.plusMillis(123 * 3), site, instrument);

        doThrow(new RuntimeException("test")).when(pipeline).process(any(), any(), any(), any());

        // 4, 5
        target.processPipeline(now);
        target.processPipeline(now);
        verifyNoMoreInteractions(pipeline);

        // 6
        target.processPipeline(now);
        verify(pipeline, times(3)).process(now, now.plusMillis(123 * 3), site, instrument);

    }

}
