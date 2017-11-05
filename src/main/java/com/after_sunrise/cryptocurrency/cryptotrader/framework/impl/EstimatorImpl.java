package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.ExecutorFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.google.inject.Inject;
import com.google.inject.Injector;

import java.math.BigDecimal;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import static java.math.BigDecimal.*;
import static java.math.RoundingMode.HALF_UP;
import static java.util.concurrent.CompletableFuture.supplyAsync;


/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class EstimatorImpl extends AbstractService implements Estimator {

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
        return WILDCARD;
    }

    @Override
    public Estimation estimate(Context context, Request request) {

        Map<Estimator, Estimation> estimations = collect(context, request);

        Estimation collapsed = collapse(estimations);

        log.info("Estimate : [{} {}] price=[{}] confidence=[{}]",
                request.getSite(), request.getInstrument(), collapsed.getPrice(), collapsed.getConfidence());

        return collapsed;

    }

    private Map<Estimator, Estimation> collect(Context context, Request request) {

        Set<String> ids = manager.getEstimators(request.getSite(), request.getInstrument());

        Map<Estimator, CompletableFuture<Estimation>> futures = new IdentityHashMap<>();

        estimators.values().stream()
                .filter(e -> ids != null)
                .filter(e -> ids.contains(WILDCARD) || ids.contains(e.get()))
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

                log.warn("Skipping estimate : " + estimator.get(), e);

            }
        });

        return estimations;

    }

    private Estimation collapse(Map<Estimator, Estimation> estimations) {

        BigDecimal numerator = BigDecimal.ZERO;

        BigDecimal denominator = BigDecimal.ZERO;

        AtomicLong total = new AtomicLong();

        for (Entry<Estimator, Estimation> entry : estimations.entrySet()) {

            Estimation estimation = entry.getValue();

            if (estimation == null || estimation.getPrice() == null || estimation.getConfidence() == null) {

                log.debug("Omitting estimate : {} ({})", estimation, entry.getKey().get());

                continue;

            }

            log.debug("Including estimate : {} - {}", estimation, entry.getKey().get());

            BigDecimal confidence = estimation.getConfidence().max(ZERO).min(ONE);

            numerator = numerator.add(estimation.getPrice().multiply(confidence));

            denominator = denominator.add(confidence);

            total.incrementAndGet();

        }

        BigDecimal price = denominator.signum() == 0 ? null : numerator.divide(denominator, SCALE, HALF_UP);

        BigDecimal confidence = total.get() == 0 ? null : denominator.divide(valueOf(total.get()), SCALE, HALF_UP);

        return Estimation.builder().price(price).confidence(confidence).build();

    }

}
