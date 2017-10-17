package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ONE;
import static org.apache.commons.lang3.math.NumberUtils.LONG_ONE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class VwapEstimator extends AbstractEstimator {

    private static final Comparator<Trade> COMPARATOR = Comparator.comparing(Trade::getTimestamp);

    private static final double SIGMA = 1.96;

    @Override
    public Estimation estimate(Context context, Request request) {

        Key key = getKey(request);

        return estimate(context, key);

    }

    protected Estimation estimate(Context context, Key key) {

        Instant now = key.getTimestamp();

        Instant from = now.minus(LONG_ONE, HOURS);

        List<Trade> trades = ofNullable(context.listTrades(key, from)).orElse(emptyList())
                .stream().filter(Objects::nonNull)
                .filter(t -> t.getTimestamp() != null)
                .filter(t -> Objects.nonNull(t.getPrice()))
                .filter(t -> Objects.nonNull(t.getSize()))
                .filter(t -> t.getSize().signum() > 0)
                .sorted(COMPARATOR)
                .collect(Collectors.toList());

        if (trades.size() == 0) {
            return BAIL; // Cannot calculate returns if less than 2 points.
        }

        if (trades.size() == 1) {

            BigDecimal price = trades.get(trades.size() - 1).getPrice();

            return Estimation.builder().price(price).confidence(ZERO).build();

        }

        double vwap = calculateVwap(trades);

        double deviation = calculateDeviation(trades);

        double last = trades.get(trades.size() - 1).getPrice().doubleValue();

        double drift = Math.min(1, Math.abs(Math.log(last / vwap) / (deviation * SIGMA)));

        BigDecimal p = BigDecimal.valueOf(vwap).setScale(SCALE, HALF_UP);

        BigDecimal c = Double.isNaN(drift) ? ZERO : BigDecimal.valueOf(1 - drift).setScale(SCALE, HALF_UP);

        log.debug("Estimated : {} (confidence=[{}] points=[{}])", p, c, trades.size());

        return Estimation.builder().price(p).confidence(c).build();

    }

    private double calculateVwap(List<Trade> trades) {

        double sumNotional = 0;

        double sumQuantity = 0;

        for (Trade t : trades) {

            sumNotional += t.getSize().multiply(t.getPrice()).doubleValue();

            sumQuantity += t.getSize().doubleValue();

        }

        return sumNotional / sumQuantity;

    }

    private double calculateDeviation(List<Trade> trades) {

        double[] rates = new double[trades.size() - 1];

        double sum = 0;

        for (int i = 1; i < trades.size(); i++) {

            double previous = trades.get(i - 1).getPrice().doubleValue();

            double current = trades.get(i).getPrice().doubleValue();

            rates[i - 1] = Math.log(current / previous);

            sum += rates[i - 1];

        }

        double average = sum / rates.length;

        double variance = Arrays.stream(rates).map(r -> r - average).map(r -> r * r).sum();

        return Math.sqrt(variance / Math.max(rates.length - INTEGER_ONE, INTEGER_ONE));

    }

}
