package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.NavigableMap;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.time.temporal.ChronoUnit.MILLIS;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class UnivariateEstimator extends AbstractEstimator {

    private static final String SAMPLES_KEY = "samples";

    private static final int SAMPLES_VAL = 60;

    @Override
    public Estimation estimate(Context context, Request request) {

        Instant now = request.getCurrentTime();

        Duration interval = Duration.between(now, request.getTargetTime());

        Instant from = request.getCurrentTime().minus(interval.toMillis() * getSamples(), MILLIS);

        List<Trade> trades = context.listTrades(getKey(context, request), from.minus(interval));

        NavigableMap<Instant, BigDecimal> prices = collapsePrices(trades, interval, from, now, false);

        NavigableMap<Instant, BigDecimal> returns = calculateReturns(prices);

        SimpleRegression regression = new SimpleRegression();

        trimToEmpty(returns).entrySet().stream()
                .filter(e -> e.getKey() != null)
                .filter(e -> e.getValue() != null)
                .forEach(e -> regression.addData(e.getKey().toEpochMilli(), e.getValue().doubleValue()));

        if (regression.getN() <= 2) {
            return BAIL;
        }

        double r = Math.exp(regression.predict(request.getTargetTime().toEpochMilli()));

        double p = r * prices.lastEntry().getValue().doubleValue();

        BigDecimal price = Double.isFinite(p) ? BigDecimal.valueOf(p).setScale(SCALE, HALF_UP) : null;

        double c = regression.getRSquare();

        BigDecimal confidence = Double.isFinite(c) ? BigDecimal.valueOf(c).setScale(SCALE, HALF_UP) : ZERO;

        log.debug("Estimated : {} (Confidence={}, Correlation={}, Samples={}, Slope={}, Intercept={})",
                price, confidence, regression.getR(), regression.getN(), regression.getSlope(), regression.getIntercept()
        );

        return Estimation.builder().price(price).confidence(confidence).build();

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
