package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static java.math.RoundingMode.HALF_UP;
import static java.time.temporal.ChronoUnit.MILLIS;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class UnivariateEstimator extends AbstractEstimator {

    private static final String SAMPLES_KEY = "samples";

    private static final int SAMPLES_VAL = 60;

    static final int I_SAMPLES = 0;

    static final int I_COEFFICIENT = 1;

    static final int I_INTERCEPT = 2;

    static final int I_CORRELATION = 3;

    static final int I_DETERMINATION = 4;

    @Override
    public Estimation estimate(Context context, Request request) {

        Instant now = request.getCurrentTime();

        Duration interval = Duration.between(now, request.getTargetTime());

        Instant from = request.getCurrentTime().minus(interval.toMillis() * getSamples(), MILLIS);

        List<Trade> trades = context.listTrades(getKey(context, request), from.minus(interval));

        NavigableMap<Instant, BigDecimal> prices = collapsePrices(trades, interval, from, now, false);

        NavigableMap<Instant, BigDecimal> returns = calculateReturns(prices);

        double[] analysis = calculate(returns);

        if (ArrayUtils.isEmpty(analysis)) {
            return BAIL;
        }

        double r = Math.exp(analysis[I_COEFFICIENT] * request.getTargetTime().toEpochMilli() + analysis[I_INTERCEPT]);

        double estimate = r * prices.lastEntry().getValue().doubleValue();

        BigDecimal price = Double.isFinite(estimate) ? BigDecimal.valueOf(estimate).setScale(SCALE, HALF_UP) : null;

        double determination = analysis[I_DETERMINATION];

        BigDecimal confidence = Double.isFinite(determination) ? BigDecimal.valueOf(determination).setScale(SCALE, HALF_UP) : null;

        log.debug("Estimated : {} (Confidence={}, Correlation={}, Samples={}, Coefficient={}, Intercept={})",
                price, confidence, analysis[I_CORRELATION], analysis[I_SAMPLES], analysis[I_COEFFICIENT], analysis[I_INTERCEPT]
        );

        return Estimation.builder().price(price).confidence(confidence).build();

    }

    @VisibleForTesting
    double[] calculate(Map<Instant, BigDecimal> values) {

        SimpleRegression regression = new SimpleRegression();

        Optional.ofNullable(values).orElse(Collections.emptyMap()).entrySet().stream()
                .filter(e -> e.getKey() != null)
                .filter(e -> e.getValue() != null)
                .forEach(e -> regression.addData(e.getKey().toEpochMilli(), e.getValue().doubleValue()));

        if (regression.getN() <= 2) {
            return null;
        }

        double[] results = new double[5];
        results[I_SAMPLES] = regression.getN();
        results[I_COEFFICIENT] = regression.getSlope();
        results[I_INTERCEPT] = regression.getIntercept();
        results[I_CORRELATION] = regression.getR();
        results[I_DETERMINATION] = regression.getRSquare();
        return results;

    }

    protected int getSamples() {
        return getIntProperty(SAMPLES_KEY, SAMPLES_VAL);
    }

    public static class Univariate005Estimator extends UnivariateEstimator {
        @Override
        protected int getSamples() {
            return 05;
        }
    }

    public static class Univariate010Estimator extends UnivariateEstimator {
        @Override
        protected int getSamples() {
            return 10;
        }
    }

    public static class Univariate015Estimator extends UnivariateEstimator {
        @Override
        protected int getSamples() {
            return 15;
        }
    }

    public static class Univariate020Estimator extends UnivariateEstimator {
        @Override
        protected int getSamples() {
            return 20;
        }
    }

    public static class Univariate030Estimator extends UnivariateEstimator {
        @Override
        protected int getSamples() {
            return 30;
        }
    }

    public static class Univariate045Estimator extends UnivariateEstimator {
        @Override
        protected int getSamples() {
            return 45;
        }
    }

    public static class Univariate060Estimator extends UnivariateEstimator {
        @Override
        protected int getSamples() {
            return 60;
        }
    }

    public static class Univariate120Estimator extends UnivariateEstimator {
        @Override
        protected int getSamples() {
            return 120;
        }
    }

    public static class Univariate240Estimator extends UnivariateEstimator {
        @Override
        protected int getSamples() {
            return 240;
        }
    }

    public static class Univariate360Estimator extends UnivariateEstimator {
        @Override
        protected int getSamples() {
            return 360;
        }
    }

    public static class Univariate480Estimator extends UnivariateEstimator {
        @Override
        protected int getSamples() {
            return 480;
        }
    }

    public static class Univariate720Estimator extends UnivariateEstimator {
        @Override
        protected int getSamples() {
            return 720;
        }
    }

}
