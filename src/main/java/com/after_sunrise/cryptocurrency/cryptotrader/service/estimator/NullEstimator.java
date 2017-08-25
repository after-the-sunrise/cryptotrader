package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import lombok.extern.slf4j.Slf4j;

import static java.math.BigDecimal.ZERO;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class NullEstimator implements Estimator {

    private static final Estimation NULL = Estimation.builder().confidence(ZERO).build();

    @Override
    public String get() {
        return getClass().getSimpleName();
    }

    @Override
    public Estimation estimate(Context context, Request request) {

        return NULL;

    }

}
