package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class LastEstimator implements Estimator {

    private static final Estimation BAIL = Estimation.builder().confidence(ZERO).build();

    @Override
    public String get() {
        return getClass().getSimpleName();
    }

    @Override
    public Estimation estimate(Context context, Request request) {

        if (context == null) {
            return BAIL;
        }

        Key key = Key.from(request);

        if (!Key.isValid(key)) {
            return BAIL;
        }

        BigDecimal price = context.getLastPrice(key);

        BigDecimal confidence = price == null ? ZERO : ONE;

        log.debug("Estimated : {} - {}", price, key);

        return Estimation.builder().price(price).confidence(confidence).build();

    }

}
