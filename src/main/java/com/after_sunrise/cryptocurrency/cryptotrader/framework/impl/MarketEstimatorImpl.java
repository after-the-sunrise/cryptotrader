package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.ExecutorFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.MarketEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.util.ServiceLoader.load;
import static java.util.concurrent.CompletableFuture.supplyAsync;


/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class MarketEstimatorImpl implements MarketEstimator {

    private final List<MarketEstimator> estimators;

    private final ExecutorService executor;

    @Inject
    public MarketEstimatorImpl(Injector injector) {

        this.estimators = Lists.newArrayList(load(MarketEstimator.class).iterator());

        this.estimators.forEach(injector::injectMembers);

        this.executor = injector.getInstance(ExecutorFactory.class).get(getClass());

    }

    @Override
    public Estimation estimate(Context context, Request request) {

        List<Estimation> estimations = collect(context, request);

        Estimation collapsed = collapse(estimations);

        return collapsed;

    }

    @VisibleForTesting
    List<Estimation> collect(Context context, Request request) {

        Map<MarketEstimator, CompletableFuture<Estimation>> futures = new IdentityHashMap<>();

        estimators.forEach(estimator ->

                futures.put(estimator, supplyAsync(() -> estimator.estimate(context, request), executor))

        );

        List<Estimation> estimations = new ArrayList<>(futures.size());

        futures.forEach((estimator, future) -> {

            try {

                Estimation estimation = future.get();

                estimations.add(estimation);

                log.debug("Intermediate Estimation : {} ({})", estimation, estimator);

            } catch (Exception e) {

                log.warn("Skipped Estimation : " + estimator, e);

            }
        });

        return estimations;

    }

    @VisibleForTesting
    Estimation collapse(List<Estimation> estimations) {

        BigDecimal total = BigDecimal.ZERO;

        BigDecimal confidences = BigDecimal.ZERO;

        for (Estimation estimation : estimations) {

            if (estimation.getPrice() == null) {
                continue;
            }

            if (estimation.getConfidence() == null) {
                continue;
            }

            total = total.add(estimation.getPrice().multiply(estimation.getConfidence()));

            confidences = confidences.add(estimation.getConfidence());

        }

        BigDecimal price = confidences.signum() == 0 ? null : total.divide(confidences);

        Estimation collapsed = Estimation.builder().price(price).confidence(confidences).build();

        log.debug("Collapsed {} estimations : {}", estimations.size(), collapsed);

        return collapsed;

    }

}
