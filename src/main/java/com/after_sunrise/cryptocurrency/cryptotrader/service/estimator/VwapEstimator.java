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

import static java.math.BigDecimal.*;
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

    private static final int SCALE = 12;

    private static final int SIGMA = 3;

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

        if (trades.isEmpty()) {
            return BAIL;
        }

        BigDecimal price;

        BigDecimal confidence;

        if (trades.size() == 1) {

            price = trades.get(0).getPrice();

            confidence = ONE;

        } else {

            double[] rates = new double[trades.size() - 1];
            double sumRates = 0;

            double totalNotional = 0;
            double totalQuantity = 0;

            for (int i = 0; i < trades.size(); i++) {

                Trade t = trades.get(i);

                totalNotional += t.getSize().multiply(t.getPrice()).doubleValue();
                totalQuantity += t.getSize().doubleValue();

                if (i == 0) {
                    continue;
                }

                double previous = trades.get(i - 1).getPrice().doubleValue();

                double current = trades.get(i).getPrice().doubleValue();

                rates[i - 1] = Math.log(current / previous);

                sumRates += rates[i - 1];

            }

            double averagePrice = totalNotional / totalQuantity;

            double variance = 0.0;

            for (double rate : rates) {

                double diff = rate - (sumRates / rates.length);

                variance += diff * diff;

            }

            double deviation = Math.sqrt(variance / Math.max(rates.length - 1, 1));

            double ratio = 1 - Math.min(deviation * SIGMA, 1);

            price = valueOf(averagePrice).setScale(SCALE, ROUND_HALF_UP);

            confidence = valueOf(ratio).setScale(SCALE, ROUND_HALF_UP);

        }

        log.debug("Estimated : {} (confidence=[{}] points=[{}])", price, confidence, trades.size());

        return Estimation.builder().price(price).confidence(confidence).build();

    }

}
