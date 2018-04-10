package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.Composite;
import com.after_sunrise.cryptocurrency.cryptotrader.core.ExecutorFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Pipeline;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class TraderImpl implements Trader {

    private final AtomicReference<CountDownLatch> tradeLatch;

    private final PropertyManager propertyManager;

    private final Pipeline pipeline;

    private final ExecutorService executor;

    private final Map<String, Map<String, AtomicLong>> frequencies;

    @Inject
    public TraderImpl(Injector injector) {

        this.tradeLatch = new AtomicReference<>(new CountDownLatch(1));

        this.propertyManager = injector.getInstance(PropertyManager.class);

        this.pipeline = injector.getInstance(Pipeline.class);

        this.frequencies = new ConcurrentHashMap<>();

        int threads = propertyManager.getTradingThreads();

        this.executor = injector.getInstance(ExecutorFactory.class).get(getClass(), threads);

    }

    @Override
    public void trigger() {

        CountDownLatch latch = new CountDownLatch(1);

        CountDownLatch old = tradeLatch.getAndSet(latch);

        if (old == null) {

            log.trace("Skipping trigger.");

            return;

        }

        log.info("Triggered.");

        old.countDown();

    }

    @Override
    public void close() {

        CountDownLatch latch = tradeLatch.getAndSet(null);

        if (latch == null) {

            log.trace("Already aborted.");

            return;

        }

        log.info("Aborted.");

        latch.countDown();

    }

    @Override
    public boolean isClosed() {
        return tradeLatch.get() == null;
    }

    @Override
    public void trade() {

        log.info("Trading started.");

        try {

            CountDownLatch latch;

            while ((latch = tradeLatch.get()) != null) {

                Instant now = propertyManager.getNow();

                log.debug("Trade attempt : {}", now);

                processPipeline(now);

                Instant finish = propertyManager.getNow();

                Duration interval = propertyManager.getTradingInterval();

                Duration elapsed = Duration.between(now, finish);

                Duration remaining = interval.minus(elapsed);

                log.debug("Sleeping : {} (Elapsed {})", remaining, elapsed);

                latch.await(Math.max(remaining.toMillis(), 0), MILLISECONDS);

            }

        } catch (Exception e) {

            log.warn("Aborting trade.", e);

        }

        log.info("Trading finished.");

    }

    @VisibleForTesting
    void processPipeline(Instant now) throws InterruptedException {

        Map<Composite, Future<?>> futures = new IdentityHashMap<>();

        for (Composite c : propertyManager.getTradingTargets()) {

            String site = trimToEmpty(c.getSite());

            String instrument = trimToEmpty(c.getInstrument());

            AtomicLong count = frequencies.computeIfAbsent(site, k -> new ConcurrentHashMap<>()).computeIfAbsent(
                    instrument,
                    k -> new AtomicLong(propertyManager.getTradingSeed(site, instrument))
            );

            Integer frequency = propertyManager.getTradingFrequency(site, instrument);

            if (count.getAndIncrement() % frequency == 0) {

                Duration interval = propertyManager.getTradingInterval();

                Instant target = now.plusMillis(Math.abs(interval.toMillis() * frequency));

                futures.put(c, executor.submit(() -> pipeline.process(now, target, site, instrument)));

            }

        }

        for (Entry<Composite, Future<?>> entry : futures.entrySet()) {

            try {

                entry.getValue().get();

            } catch (ExecutionException e) {

                log.error("Trading failure : " + entry.getKey(), e);

            }

        }

    }

}
