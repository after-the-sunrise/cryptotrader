package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;

import static java.math.BigDecimal.ZERO;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public abstract class AbstractEstimator implements Estimator {

    protected static final Estimation BAIL = Estimation.builder().confidence(ZERO).build();

    protected Key getKey(Request request) {
        return Key.from(request);
    }

}
