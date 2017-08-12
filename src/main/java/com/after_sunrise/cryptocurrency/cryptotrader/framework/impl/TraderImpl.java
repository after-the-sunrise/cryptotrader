package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.PipelineProcessor;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class TraderImpl implements Trader {

    private final AtomicReference<CountDownLatch> tradeLatch;

    private final PropertyManager propertyManager;

    private final PipelineProcessor pipelineProcessor;

    @Inject
    public TraderImpl(Injector injector) {

        this.tradeLatch = new AtomicReference<>(new CountDownLatch(1));

        this.propertyManager = injector.getInstance(PropertyManager.class);

        this.pipelineProcessor = injector.getInstance(PipelineProcessor.class);

    }

    @Override
    public void trade() {

        try {

            CountDownLatch latch;

            while ((latch = tradeLatch.get()) != null) {

                Instant now = propertyManager.getNow();

                BigDecimal level = propertyManager.getTradingAggressiveness();

                Request.RequestBuilder builder = Request.builder().timestamp(now).aggressiveness(level);

                propertyManager.getTradingTargets().forEach((site, instruments) -> {

                    instruments.forEach(instrument -> {

                        Request request = builder.site(site).instrument(instrument).build();

                        log.debug("Trading : {}", request);

                        pipelineProcessor.accept(request);

                    });

                });

                Duration interval = propertyManager.getTradingInterval();

                if (interval == null) {

                    log.debug("No more interval.");

                    break;

                }

                log.debug("Sleeping for interval : {}", interval);

                latch.await(interval.toMillis(), MILLISECONDS);

            }

        } catch (Exception e) {

            log.warn("Aborting trade.", e);

        }

    }

}
