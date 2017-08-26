package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.ExecutorFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.math.BigDecimal.valueOf;
import static java.math.RoundingMode.HALF_UP;
import static java.util.concurrent.CompletableFuture.supplyAsync;


/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class EstimatorImpl implements Estimator {

    private final ExecutorService executor;

    private final PropertyManager manager;

    private final Map<String, Estimator> estimators;

    @Inject
    public EstimatorImpl(Injector injector) {

        this.estimators = injector.getInstance(ServiceFactory.class).loadMap(Estimator.class);

        this.manager = injector.getInstance(PropertyManager.class);

        this.executor = injector.getInstance(ExecutorFactory.class).get(getClass(), estimators.size());

    }

    @Override
    public String get() {
        return Service.WILDCARD;
    }

    @Override
    public Estimation estimate(Context context, Request request) {

        Map<Estimator, Estimation> estimations = collect(context, request);

        Estimation collapsed = collapse(estimations);

        return collapsed;

    }

    private Map<Estimator, Estimation> collect(Context context, Request request) {

        Set<String> ids = manager.getEstimators(request.getSite(), request.getInstrument());

        Map<Estimator, CompletableFuture<Estimation>> futures = new IdentityHashMap<>();

        estimators.values().stream()
                .filter(e -> ids != null)
                .filter(e -> ids.contains(Service.WILDCARD) || ids.contains(e.get()))
                .forEach(estimator ->
                        futures.put(estimator,
                                supplyAsync(() -> estimator.estimate(context, request), executor)
                        )
                );

        Map<Estimator, Estimation> estimations = new IdentityHashMap<>();

        futures.forEach((estimator, future) -> {

            try {

                Estimation estimation = future.get();

                estimations.put(estimator, estimation);

            } catch (Exception e) {

                log.warn("Skipping estimate : " + estimator, e);

            }
        });

        return estimations;

    }

    private Estimation collapse(Map<Estimator, Estimation> estimations) {

        BigDecimal numerator = BigDecimal.ZERO;

        BigDecimal denominator = BigDecimal.ZERO;

        AtomicLong total = new AtomicLong();

        AtomicInteger s = new AtomicInteger(SCALE);

        for (Entry<Estimator, Estimation> entry : estimations.entrySet()) {

            Estimation estimation = entry.getValue();

            if (estimation == null || estimation.getPrice() == null || estimation.getConfidence() == null) {

                log.debug("Omitting estimate : {} ({})", estimation, entry.getKey());

                continue;

            }

            log.debug("Including estimate : {} - {}", estimation, entry.getKey());

            numerator = numerator.add(estimation.getPrice().multiply(estimation.getConfidence()));

            denominator = denominator.add(estimation.getConfidence());

            total.incrementAndGet();

            s.set(Math.max(estimation.getPrice().scale(), s.get()));

        }

        BigDecimal price = denominator.signum() == 0 ? null : numerator.divide(denominator, s.get(), HALF_UP);

        BigDecimal confidence = total.get() == 0 ? null : denominator.divide(valueOf(total.get()), s.get(), HALF_UP);

        Estimation collapsed = Estimation.builder().price(price).confidence(confidence).build();

        log.info("Collapsed {} estimations : {} (= {} / {})", total, collapsed, numerator, denominator);

        return collapsed;

    }

}
