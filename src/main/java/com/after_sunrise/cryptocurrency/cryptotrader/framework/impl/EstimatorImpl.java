package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.ExecutorFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import static java.math.BigDecimal.*;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Optional.ofNullable;
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

        Map<String, BigDecimal> ids = getConfiguredEstimators(request.getSite(), request.getInstrument());

        Map<Estimator, Estimation> estimations = collect(context, request, ids);

        Estimation collapsed = collapse(estimations, ids);

        log.info("Estimate : [{} {}] price=[{}] confidence=[{}]",
                request.getSite(), request.getInstrument(), collapsed.getPrice(), collapsed.getConfidence());

        return collapsed;

    }

    private Map<String, BigDecimal> getConfiguredEstimators(String site, String instrument) {

        Collection<String> ids = manager.getEstimators(site, instrument);

        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }

        Map<String, BigDecimal> map = new HashMap<>();

        ids.stream()
                .filter(StringUtils::isNotEmpty)
                .map(id -> StringUtils.split(id, ":", 2))
                .forEach(s -> map.put(s[0], s.length <= 1 ? null : new BigDecimal(s[1])));

        return map;

    }

    private Map<Estimator, Estimation> collect(Context context, Request request, Map<String, BigDecimal> ids) {

        Map<Estimator, CompletableFuture<Estimation>> futures = new IdentityHashMap<>();

        estimators.values().stream()
                .filter(e -> ids != null)
                .filter(e -> ids.containsKey(WILDCARD) || ids.containsKey(e.get()))
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

    private Estimation collapse(Map<Estimator, Estimation> estimations, Map<String, BigDecimal> ids) {

        BigDecimal numerator = BigDecimal.ZERO;

        BigDecimal denominator = BigDecimal.ZERO;

        AtomicLong total = new AtomicLong();

        for (Entry<Estimator, Estimation> entry : estimations.entrySet()) {

            String id = entry.getKey().get();

            Estimation estimation = entry.getValue();

            if (estimation == null || estimation.getPrice() == null || estimation.getConfidence() == null) {

                log.debug("Omitting estimate : {} ({})", estimation, id);

                continue;

            }

            BigDecimal multiplier = ofNullable(ids.get(id)).orElse(ONE);

            log.debug("Including estimate : {} - {} (x{})", estimation, id, multiplier);

            BigDecimal confidence = estimation.getConfidence().multiply(multiplier).max(ZERO).min(ONE);

            numerator = numerator.add(estimation.getPrice().multiply(confidence));

            denominator = denominator.add(confidence);

            total.incrementAndGet();

        }

        BigDecimal price = denominator.signum() == 0 ? null : numerator.divide(denominator, SCALE, HALF_UP);

        BigDecimal confidence = total.get() == 0 ? null : denominator.divide(valueOf(total.get()), SCALE, HALF_UP);

        return Estimation.builder().price(price).confidence(confidence).build();

    }

}
