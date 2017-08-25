package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
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

    private static final int SCALE = 12;

    private static final int SIGMA = 6;

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
                .filter(t -> Objects.nonNull(t.getPrice()))
                .filter(t -> Objects.nonNull(t.getSize()))
                .filter(t -> t.getSize().signum() > 0)
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

            double totalNotional = 0;
            double totalQuantity = 0;

            for (Trade t : trades) {
                totalNotional += t.getSize().doubleValue() * t.getPrice().doubleValue();
                totalQuantity += t.getSize().doubleValue();
            }

            double averagePrice = totalNotional / totalQuantity;

            double variance = 0.0;

            for (Trade t : trades) {
                variance += Math.pow(t.getPrice().doubleValue() - averagePrice, 2);
            }

            double deviation = Math.sqrt(variance / (trades.size() - 1));

            double ratio = 1 - Math.max(Math.min(deviation * SIGMA / averagePrice, 1), 0);

            price = valueOf(averagePrice).setScale(SCALE, ROUND_HALF_UP);

            confidence = valueOf(ratio).setScale(SCALE, ROUND_HALF_UP);

        }

        log.debug("Estimated : {} (confidence=[{}] points=[{}])", price, confidence, trades.size());

        return Estimation.builder().price(price).confidence(confidence).build();

    }

}
