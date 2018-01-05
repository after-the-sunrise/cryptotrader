package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.HALF_UP;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class AdviserImpl extends AbstractService implements Adviser {

    private final Advice BAIL = Advice.builder().build();

    private final Map<String, Adviser> advisers;

    @Inject
    public AdviserImpl(Injector injector) {

        this.advisers = injector.getInstance(ServiceFactory.class).loadMap(Adviser.class);

    }

    @Override
    public String get() {
        return WILDCARD;
    }

    @Override
    public Advice advise(Context context, Request request, Estimation estimation) {

        Adviser adviser = advisers.get(request.getSite());

        if (adviser == null) {

            log.debug("Service not found : {}", request.getSite());

            return BAIL;

        }

        Advice advice = adviser.advise(context, request, estimation);

        BigDecimal b = calculateBasis(estimation, advice, Advice::getBuyLimitPrice, ONE.negate());

        BigDecimal s = calculateBasis(estimation, advice, Advice::getSellLimitPrice, ONE);

        log.debug("Advice : [{}.{}] bBasis=[{}] sBasis=[{}]", request.getSite(), request.getInstrument(), b, s);

        log.info("Advice : [{}.{}] {}", request.getSite(), request.getInstrument(), advice);

        return Optional.ofNullable(advice).orElse(BAIL);

    }

    @VisibleForTesting
    BigDecimal calculateBasis(Estimation estimation, Advice advice, Function<Advice, BigDecimal> f, BigDecimal m) {

        if (estimation == null || advice == null) {
            return null;
        }

        BigDecimal estimate = estimation.getPrice();

        if (estimate == null || estimate.signum() == 0) {
            return null;
        }

        BigDecimal price = f.apply(advice);

        if (price == null) {
            return null;
        }

        return price.divide(estimate, SCALE, HALF_UP).subtract(ONE).multiply(m);

    }

}
