package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.math.NumberUtils.LONG_ONE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class VwapEstimator implements Estimator {

    private static final Estimation BAIL = Estimation.builder().confidence(ZERO).build();

    private static final Comparator<Trade> COMPARATOR = Comparator.comparing(Trade::getTimestamp);

    private static final double SIGMA = 1.96;

    @Override
    public String get() {
        return getClass().getSimpleName();
    }

    @Override
    public Estimation estimate(Context context, Request request) {

        Instant now = request.getCurrentTime();

        Instant from = now.minus(LONG_ONE, DAYS);

        List<Trade> trades = ofNullable(context.listTrades(Key.from(request), from)).orElse(emptyList())
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

        double sumNotional = 0;
        double sumQuantity = 0;

        double[] rates = new double[trades.size() - 1];
        double sumRates = 0;

        for (int i = 0; i < trades.size(); i++) {

            Trade t = trades.get(i);
            sumNotional += t.getSize().multiply(t.getPrice()).doubleValue();
            sumQuantity += t.getSize().doubleValue();

            if (i == 0) {
                continue;
            }

            double previous = trades.get(i - 1).getPrice().doubleValue();
            double current = trades.get(i).getPrice().doubleValue();

            rates[i - 1] = Math.log(current / previous);
            sumRates += rates[i - 1];

        }

        double vwap = sumNotional / sumQuantity;

        double variance = 0.0;

        for (double rate : rates) {

            double diff = rate - (sumRates / rates.length);

            variance += diff * diff;

        }

        double deviation = Math.sqrt(variance / Math.max(rates.length - 1, 1));

        double last = trades.get(trades.size() - 1).getPrice().doubleValue();

        double rate = Math.log(last / vwap);

        double drift = Math.min(1, Math.abs(rate / (deviation * SIGMA)));

        BigDecimal p = BigDecimal.valueOf(vwap).setScale(SCALE, HALF_UP);

        BigDecimal c = Double.isNaN(drift) ? ZERO : BigDecimal.valueOf(1 - drift).setScale(SCALE, HALF_UP);

        log.debug("Estimated : {} (confidence=[{}] points=[{}])", p, c, trades.size());

        return Estimation.builder().price(p).confidence(c).build();

    }

}
