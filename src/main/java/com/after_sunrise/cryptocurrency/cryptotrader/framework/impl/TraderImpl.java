package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.CompletableFuture.allOf;
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

                Instant time = calculateTime();

                log.debug("Trade attempt : {}", time);

                List<CompletableFuture<?>> futures = new ArrayList<>();

                propertyManager.getTradingTargets().forEach(
                        (site, instruments) -> instruments.forEach(
                                i -> futures.add(processPipeline(time, site, i))
                        ));

                allOf(futures.toArray(new CompletableFuture[futures.size()])).get();

                Duration interval = calculateInterval(time);

                log.debug("Sleeping for interval : {}", interval);

                latch.await(interval.toMillis(), MILLISECONDS);

            }

        } catch (Exception e) {

            log.warn("Aborting trade.", e);

        }

        log.info("Trading finished.");

    }

    @VisibleForTesting
    Instant calculateTime() {

        Duration interval = propertyManager.getTradingInterval();

        Instant now = propertyManager.getNow();

        return interval == null ? now : now.plus(interval);

    }

    @VisibleForTesting
    Duration calculateInterval(Instant target) {

        if (target == null) {
            return Duration.ZERO;
        }

        Instant now = propertyManager.getNow();

        if (now.isAfter(target)) {
            return Duration.ZERO;
        }

        return Duration.between(now, target);

    }

    @VisibleForTesting
    CompletableFuture<?> processPipeline(Instant time, String site, String instrument) {

        return CompletableFuture.runAsync(() -> {

            String s = trimToEmpty(site);

            Map<String, AtomicLong> instruments = frequencies.computeIfAbsent(s, k -> new ConcurrentHashMap<>());

            String i = trimToEmpty(instrument);

            AtomicLong count = instruments.computeIfAbsent(i, k -> new AtomicLong());

            Integer frequency = propertyManager.getTradingFrequency(site, instrument);

            if (count.getAndIncrement() % frequency == 0) {

                pipeline.process(time, site, instrument);

            } else {

                log.debug("Skipping : {} : {}", site, instrument);

            }


        }, executor);

    }

}
