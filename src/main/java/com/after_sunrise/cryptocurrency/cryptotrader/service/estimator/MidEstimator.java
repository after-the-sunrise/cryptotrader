package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class MidEstimator implements Estimator {

    private static final Estimation BAIL = Estimation.builder().confidence(ZERO).build();

    private static final BigDecimal HALF = new BigDecimal("0.5");

    @Override
    public String get() {
        return getClass().getSimpleName();
    }

    @Override
    public Estimation estimate(Context context, Request request) {

        Key key = Key.from(request);

        BigDecimal price = context.getMidPrice(key);

        BigDecimal confidence = price == null ? ZERO : HALF;

        log.debug("Estimated : {} - {}", price, key);

        return Estimation.builder().price(price).confidence(confidence).build();

    }

}
