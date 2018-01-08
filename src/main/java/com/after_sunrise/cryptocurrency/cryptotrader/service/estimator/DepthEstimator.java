package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.google.common.annotations.VisibleForTesting;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.math.BigDecimal.*;
import static java.math.RoundingMode.HALF_UP;
import static java.time.temporal.ChronoUnit.MILLIS;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class DepthEstimator extends AbstractEstimator {

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

        double[] averages = {0.0, 0.0};

        BigDecimal ceiling = mid.add(deviation);

        trimToEmpty(context.getAskPrices(key)).entrySet().stream()
                .filter(e -> e.getKey() != null)
                .filter(e -> e.getKey().signum() > 0)
                .filter(e -> e.getKey().compareTo(ceiling) <= 0)
                .filter(e -> e.getValue() != null)
                .filter(e -> e.getValue().signum() > 0)
                .forEach(e -> {
                    averages[0] = averages[0] + e.getValue().doubleValue() * e.getKey().doubleValue();
                    averages[1] = averages[1] + e.getValue().doubleValue();
                });

        BigDecimal floor = mid.subtract(deviation);

        trimToEmpty(context.getBidPrices(key)).entrySet().stream()
                .filter(e -> e.getKey() != null)
                .filter(e -> e.getKey().signum() > 0)
                .filter(e -> e.getKey().compareTo(floor) >= 0)
                .filter(e -> e.getValue() != null)
                .filter(e -> e.getValue().signum() > 0)
                .forEach(e -> {
                    averages[0] = averages[0] + e.getValue().doubleValue() * e.getKey().doubleValue();
                    averages[1] = averages[1] + e.getValue().doubleValue();
                });

        double average = averages[0] / averages[1];

        if (!Double.isFinite(average)) {
            return BAIL;
        }

        BigDecimal d = valueOf(average).subtract(mid);

        BigDecimal p = mid.subtract(d);

        BigDecimal c = ONE.subtract(d.divide(deviation, SCALE, HALF_UP).abs()).max(ONE).min(ZERO);

        log.debug("Estimated : {} (confidence=[{}] mid=[{}] dev=[{}] avg=[{}])", p, c, mid, deviation, average);

        return Estimation.builder().price(p).confidence(c).build();

    }

    @VisibleForTesting
    BigDecimal calculateDeviation(Context context, Request request) {

        Instant now = request.getCurrentTime();

        Duration interval = Duration.between(now, request.getTargetTime());

        Instant from = request.getCurrentTime().minus(interval.toMillis() * getSamples(), MILLIS);

        List<Trade> trades = context.listTrades(getKey(context, request), from.minus(interval));

        List<BigDecimal> prices = trimToEmpty(trades).stream()
                .filter(Objects::nonNull)
                .filter(t -> t.getPrice() != null)
                .filter(t -> t.getPrice().signum() != 0)
                .filter(t -> t.getSize() != null)
                .filter(t -> t.getSize().signum() != 0)
                .map(Trade::getPrice)
                .collect(Collectors.toList());

        if (prices.size() <= 1) {
            return null;
        }

        BigDecimal sum = prices.stream().reduce(ZERO, BigDecimal::add);

        BigDecimal avg = sum.divide(valueOf(prices.size()), SCALE, HALF_UP);

        BigDecimal vrc = prices.stream().map(p -> p.subtract(avg).pow(2)).reduce(ZERO, BigDecimal::add)
                .divide(valueOf(trades.size() - 1), SCALE, HALF_UP);

        BigDecimal dev = valueOf(Math.sqrt(vrc.doubleValue())).setScale(SCALE, HALF_UP);

        return dev.multiply(getSigma(trades.size() - 1));

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
