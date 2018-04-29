package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class KeyPreloader extends AbstractEstimator {

    private final ExecutorService executor;

    public KeyPreloader() {

        executor = Executors.newFixedThreadPool(Byte.MAX_VALUE, new ThreadFactory() {

            private final Logger log = LoggerFactory.getLogger(KeyPreloader.class);

            private final AtomicLong counter = new AtomicLong();

            private final ThreadFactory delegate = Executors.defaultThreadFactory();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = delegate.newThread(r);
                t.setDaemon(true);
                t.setName(String.format("%s_%04d", KeyPreloader.class.getSimpleName(), counter.incrementAndGet()));
                t.setUncaughtExceptionHandler((d, e) -> log.warn("Preload failed : {} - {}", d.getName(), e));
                return t;
            }

        });

    }

    @Override
    public Estimation estimate(Context context, Request request) {

        Instant cutoff = request.getTargetTime();

        Key key = getKey(context, request);

        schedule(cutoff, executor, () -> context.getState(key));

        schedule(cutoff, executor, () -> context.getBestAskPrice(key));

        schedule(cutoff, executor, () -> context.getBestBidPrice(key));

        schedule(cutoff, executor, () -> context.getBestAskSize(key));

        schedule(cutoff, executor, () -> context.getBestBidSize(key));

        schedule(cutoff, executor, () -> context.getMidPrice(key));

        schedule(cutoff, executor, () -> context.getLastPrice(key));

        schedule(cutoff, executor, () -> context.getAskPrices(key));

        schedule(cutoff, executor, () -> context.getBidPrices(key));

        schedule(cutoff, executor, () -> context.listTrades(key, null));

        schedule(cutoff, executor, () -> context.getInstrumentCurrency(key));

        schedule(cutoff, executor, () -> context.getFundingCurrency(key));

        schedule(cutoff, executor, () -> context.findProduct(key, null, null));

        schedule(cutoff, executor, () -> context.getConversionPrice(key, null));

        schedule(cutoff, executor, () -> context.getInstrumentPosition(key));

        schedule(cutoff, executor, () -> context.getFundingPosition(key));

        schedule(cutoff, executor, () -> context.roundLotSize(key, null, null));

        schedule(cutoff, executor, () -> context.roundTickSize(key, null, null));

        schedule(cutoff, executor, () -> context.getCommissionRate(key));

        schedule(cutoff, executor, () -> context.isMarginable(key));

        schedule(cutoff, executor, () -> context.getExpiry(key));

        schedule(cutoff, executor, () -> context.findOrder(key, null));

        schedule(cutoff, executor, () -> context.listActiveOrders(key));

        schedule(cutoff, executor, () -> context.listExecutions(key));

        return BAIL;

    }

    @VisibleForTesting
    void schedule(Instant cutoff, ExecutorService executor, Runnable runnable) {

        if (cutoff == null) {
            return;
        }

        executor.execute(() -> {

            if (cutoff.isBefore(Instant.now())) {
                return;
            }

            runnable.run();

        });

    }

}
