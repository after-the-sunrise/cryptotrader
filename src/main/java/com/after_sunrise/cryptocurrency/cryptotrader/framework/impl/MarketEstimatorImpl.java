package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.ExecutorFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.MarketEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.HALF_UP;
import static java.util.concurrent.CompletableFuture.supplyAsync;


/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class MarketEstimatorImpl implements MarketEstimator {

    private static final int SCALE = 10;

    private final List<MarketEstimator> estimators;

    private final ExecutorService executor;

    @Inject
    public MarketEstimatorImpl(Injector injector) {

        this.estimators = Frameworks.load(MarketEstimator.class, injector);

        this.executor = injector.getInstance(ExecutorFactory.class).get(getClass());

    }

    @Override
    public Estimation estimate(Context context, Request request) {

        Map<MarketEstimator, Estimation> estimations = collect(context, request);

        Estimation collapsed = collapse(estimations);

        return collapsed;

    }

    @VisibleForTesting
    Map<MarketEstimator, Estimation> collect(Context context, Request request) {

        Map<MarketEstimator, CompletableFuture<Estimation>> futures = new IdentityHashMap<>();

        estimators.forEach(estimator ->

                futures.put(estimator, supplyAsync(() -> estimator.estimate(context, request), executor))

        );

        Map<MarketEstimator, Estimation> estimations = new IdentityHashMap<>();

        futures.forEach((estimator, future) -> {

            try {

                Estimation estimation = future.get();

                estimations.put(estimator, estimation);

                log.trace("Intermediate Estimation : {} ({})", estimation, estimator);

            } catch (Exception e) {

                log.trace("Skipped Estimation : " + estimator, e);

            }
        });

        return estimations;

    }

    @VisibleForTesting
    Estimation collapse(Map<MarketEstimator, Estimation> estimations) {

        BigDecimal numerator = BigDecimal.ZERO;

        BigDecimal denominator = BigDecimal.ZERO;

        BigDecimal total = BigDecimal.ZERO;

        for (Entry<MarketEstimator, Estimation> entry : estimations.entrySet()) {

            Estimation estimation = entry.getValue();

            if (estimation == null || estimation.getPrice() == null || estimation.getConfidence() == null) {

                log.trace("Ignoring estimation : {} ({})", estimation, entry.getKey());

                continue;

            }

            numerator = numerator.add(estimation.getPrice().multiply(estimation.getConfidence()));

            denominator = denominator.add(estimation.getConfidence());

            total = total.add(ONE);

        }

        BigDecimal price = denominator.signum() == 0 ? null : numerator.divide(denominator, SCALE, HALF_UP);

        BigDecimal confidence = total.signum() == 0 ? null : denominator.divide(total, SCALE, HALF_UP);

        Estimation collapsed = Estimation.builder().price(price).confidence(confidence).build();

        log.debug("Collapsed {} estimations : {} (= {} / {})", total, collapsed, numerator, denominator);

        return collapsed;

    }

}
