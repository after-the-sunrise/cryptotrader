package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.math.NumberUtils.LONG_ONE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class LastEstimator extends AbstractEstimator {

    private static final Comparator<Trade> COMPARATOR = Comparator.comparing(Trade::getTimestamp).reversed();

    @Override
    public Estimation estimate(Context context, Request request) {

        Key key = getKey(request);

        return estimate(context, key);

    }

    protected Estimation estimate(Context context, Key key) {

        Instant now = key.getTimestamp();

        Instant from = now.minus(LONG_ONE, HOURS);

        Optional<Trade> value = ofNullable(context.listTrades(key, from))
                .orElse(emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(t -> Objects.nonNull(t.getTimestamp()))
                .filter(t -> Objects.nonNull(t.getPrice()))
                .sorted(COMPARATOR)
                .findFirst();

        if (!value.isPresent()) {
            return BAIL;
        }

        Instant time = value.get().getTimestamp();

        BigDecimal confidence = calculateConfidence(time, now);

        BigDecimal price = value.get().getPrice();

        log.debug("Estimated : {} (confidence=[{}] time=[{}])", price, confidence, time);

        return Estimation.builder().price(price).confidence(confidence).build();

    }

    @VisibleForTesting
    BigDecimal calculateConfidence(Instant time, Instant now) {

        Duration duration = Duration.between(time, now);

        if (duration.isZero() || duration.isNegative()) {
            return ONE;
        }

        // Number from 0 to 29 (= 100 years)
        double scaled = Math.log(duration.toMillis());

        // Number from 0.00 to 0.79
        double multiplied = scaled * Math.E / 100;

        // Number from 100.00 to 0.21
        double confidence = 1 - multiplied;

        // Sanitize in case if +3000 years...
        return BigDecimal.valueOf(confidence).max(ZERO).setScale(SCALE, HALF_UP);

    }

}
