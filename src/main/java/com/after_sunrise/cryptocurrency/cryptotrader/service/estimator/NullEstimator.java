package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class NullEstimator extends AbstractEstimator {

    @Override
    public Estimation estimate(Context context, Request request) {
        return BAIL;
    }

}
