package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.core.Composite;
import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Pipeline;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;
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

        Instant t = Instant.now();
        AtomicInteger a = new AtomicInteger();
        when(module.getMock(PropertyManager.class).getNow()).thenReturn(t, t.plusMillis(30), t, t.plusSeconds(3));
        when(module.getMock(PropertyManager.class).getTradingInterval()).thenReturn(Duration.ofMillis(50));
        when(module.getMock(PropertyManager.class).getTradingExtension()).thenReturn(2);

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
    public void testProcessDuration() {

        Queue<Duration> durations = new LinkedList<>();
        when(module.getMock(PropertyManager.class).getTradingExtension()).thenReturn(3);

        target.processDuration(durations, Duration.ofMillis(1));
        assertEquals(durations.size(), 1);
        assertTrue(durations.contains(Duration.ofMillis(1)));

        target.processDuration(durations, Duration.ofMillis(2));
        assertEquals(durations.size(), 2);
        assertTrue(durations.contains(Duration.ofMillis(1)));
        assertTrue(durations.contains(Duration.ofMillis(2)));

        target.processDuration(durations, Duration.ofMillis(3));
        assertEquals(durations.size(), 3);
        assertTrue(durations.contains(Duration.ofMillis(1)));
        assertTrue(durations.contains(Duration.ofMillis(2)));
        assertTrue(durations.contains(Duration.ofMillis(3)));

        target.processDuration(durations, Duration.ofMillis(4));
        assertEquals(durations.size(), 3);
        assertTrue(durations.contains(Duration.ofMillis(2)));
        assertTrue(durations.contains(Duration.ofMillis(3)));
        assertTrue(durations.contains(Duration.ofMillis(4)));

        target.processDuration(durations, Duration.ofMillis(1));
        assertEquals(durations.size(), 3);
        assertTrue(durations.contains(Duration.ofMillis(3)));
        assertTrue(durations.contains(Duration.ofMillis(4)));
        assertTrue(durations.contains(Duration.ofMillis(1)));

        when(module.getMock(PropertyManager.class).getTradingExtension()).thenReturn(-1);
        target.processDuration(durations, Duration.ofMillis(1));
        assertEquals(durations.size(), 0);

    }

    @Test(timeOut = 5000)
    public void testCalculateInterval() {

        Queue<Duration> durations = new LinkedList<>();
        when(module.getMock(PropertyManager.class).getTradingInterval()).thenReturn(Duration.ofMillis(50));

        // Empty Durations
        durations.clear();
        assertEquals(target.calculateInterval(durations), Duration.ofMillis(50));

        // Average = (40) / 1 = 40
        durations.clear();
        durations.add(Duration.ofMillis(40));
        assertEquals(target.calculateInterval(durations), Duration.ofMillis(50));

        // Average = (40 + 80) / 2 = 60
        durations.clear();
        durations.add(Duration.ofMillis(40));
        durations.add(Duration.ofMillis(80));
        assertEquals(target.calculateInterval(durations), Duration.ofMillis(50 + 10));

        // Average = (40 + 80 + 70) / 3 = 63.3333...
        durations.clear();
        durations.add(Duration.ofMillis(40));
        durations.add(Duration.ofMillis(80));
        durations.add(Duration.ofMillis(70));
        assertEquals(target.calculateInterval(durations), Duration.ofMillis(50 + 13));

        // Average = (70 + 85 + 95) / 3 = 83.3333...
        durations.clear();
        durations.add(Duration.ofMillis(70));
        durations.add(Duration.ofMillis(85));
        durations.add(Duration.ofMillis(95));
        assertEquals(target.calculateInterval(durations), Duration.ofMillis(50 + 33));

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
