package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;

import static java.math.BigDecimal.*;
import static java.math.RoundingMode.HALF_UP;
import static java.time.temporal.ChronoUnit.MILLIS;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class DepthEstimator extends AbstractEstimator {

    private static final int I_NOTIONAL = 0;
    private static final int I_QUANTITY = 1;
    private static final int I_SIZE_ASK = 2;
    private static final int I_SIZE_BID = 3;

    private static final String SAMPLES_KEY = "samples";

    private static final int SAMPLES_VAL = 60;

    @Override
    public Estimation estimate(Context context, Request request) {

        Context.Key key = getKey(context, request);

        BigDecimal mid = context.getMidPrice(key);

        if (mid == null) {
            return BAIL;
        }

        BigDecimal deviation = calculateDeviation(context, request);

        if (deviation == null || deviation.signum() <= 0) {
            return BAIL;
        }

        double[] averages = new double[4];

        BigDecimal ceiling = mid.multiply(ONE.add(deviation));

        trimToEmpty(context.getAskPrices(key)).entrySet().stream()
                .filter(e -> e.getValue() != null)
                .filter(e -> e.getKey() != null)
                .filter(e -> e.getKey().compareTo(ceiling) <= 0)
                .forEach(e -> {
                    averages[I_NOTIONAL] = averages[I_NOTIONAL] + e.getValue().doubleValue() * e.getKey().doubleValue();
                    averages[I_QUANTITY] = averages[I_QUANTITY] + e.getValue().doubleValue();
                    averages[I_SIZE_ASK] = averages[I_SIZE_ASK] + e.getValue().doubleValue();
                });

        BigDecimal floor = mid.multiply(ONE.subtract(deviation));

        trimToEmpty(context.getBidPrices(key)).entrySet().stream()
                .filter(e -> e.getValue() != null)
                .filter(e -> e.getKey() != null)
                .filter(e -> e.getKey().compareTo(floor) >= 0)
                .forEach(e -> {
                    averages[I_NOTIONAL] = averages[I_NOTIONAL] + e.getValue().doubleValue() * e.getKey().doubleValue();
                    averages[I_QUANTITY] = averages[I_QUANTITY] + e.getValue().doubleValue();
                    averages[I_SIZE_BID] = averages[I_SIZE_BID] + e.getValue().doubleValue();
                });

        double average = averages[I_NOTIONAL] / averages[I_QUANTITY];

        if (!Double.isFinite(average)) {
            return BAIL;
        }

        BigDecimal d = valueOf(average).subtract(mid);

        BigDecimal p = mid.subtract(d);

        BigDecimal c = ONE.subtract(deviation).min(ONE).max(ZERO).multiply(HALF);

        log.debug("Estimated : {} (confidence=[{}] mid=[{}] dev=[{}] avg=[{}] askVol=[{}] bidVol=[{}])",
                p, c, mid, deviation, average, averages[I_SIZE_ASK], averages[I_SIZE_BID]);

        return Estimation.builder().price(p).confidence(c).build();

    }

    @VisibleForTesting
    BigDecimal calculateDeviation(Context context, Request request) {

        Instant to = request.getCurrentTime();

        Duration interval = Duration.between(to, request.getTargetTime());

        Instant from = request.getCurrentTime().minus(interval.toMillis() * getSamples(), MILLIS);

        List<Trade> trades = context.listTrades(getKey(context, request), from.minus(interval));

        NavigableMap<Instant, BigDecimal> prices = collapsePrices(trades, interval, from, to, false);

        NavigableMap<Instant, BigDecimal> returns = calculateReturns(prices);

        SummaryStatistics stats = new SummaryStatistics();

        returns.values().stream().filter(Objects::nonNull).forEach(r -> stats.addValue(r.doubleValue()));

        if (stats.getN() <= 1) {
            return null;
        }

        double avg = stats.getMean();

        double dev = stats.getStandardDeviation();

        double sum = Math.abs(avg) + (dev * getSigma(stats.getN() - 1).doubleValue());

        return Double.isFinite(sum) ? BigDecimal.valueOf(sum).setScale(SCALE, HALF_UP) : null;

    }

    protected int getSamples() {
        return getIntProperty(SAMPLES_KEY, SAMPLES_VAL);
    }

    public static class Depth001Estimator extends DepthEstimator {
        @Override
        protected int getSamples() {
            return 01;
        }
    }

    public static class Depth003Estimator extends DepthEstimator {
        @Override
        protected int getSamples() {
            return 03;
        }
    }

    public static class Depth005Estimator extends DepthEstimator {
        @Override
        protected int getSamples() {
            return 05;
        }
    }

    public static class Depth010Estimator extends DepthEstimator {
        @Override
        protected int getSamples() {
            return 10;
        }
    }

    public static class Depth015Estimator extends DepthEstimator {
        @Override
        protected int getSamples() {
            return 15;
        }
    }

    public static class Depth020Estimator extends DepthEstimator {
        @Override
        protected int getSamples() {
            return 20;
        }
    }

    public static class Depth030Estimator extends DepthEstimator {
        @Override
        protected int getSamples() {
            return 30;
        }
    }

    public static class Depth045Estimator extends DepthEstimator {
        @Override
        protected int getSamples() {
            return 45;
        }
    }

    public static class Depth060Estimator extends DepthEstimator {
        @Override
        protected int getSamples() {
            return 60;
        }
    }

    public static class Depth120Estimator extends DepthEstimator {
        @Override
        protected int getSamples() {
            return 120;
        }
    }

    public static class Depth240Estimator extends DepthEstimator {
        @Override
        protected int getSamples() {
            return 240;
        }
    }

    public static class Depth360Estimator extends DepthEstimator {
        @Override
        protected int getSamples() {
            return 360;
        }
    }

    public static class Depth480Estimator extends DepthEstimator {
        @Override
        protected int getSamples() {
            return 480;
        }
    }

    public static class Depth720Estimator extends DepthEstimator {
        @Override
        protected int getSamples() {
            return 720;
        }
    }

}
