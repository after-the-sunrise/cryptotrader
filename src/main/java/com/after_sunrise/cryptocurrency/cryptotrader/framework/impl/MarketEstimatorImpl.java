package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.MarketEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import lombok.extern.slf4j.Slf4j;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.MarketEstimator.Estimation.Type.BAILED;


/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class MarketEstimatorImpl implements MarketEstimator {

    @Override
    public Estimation estimate(Context context, Request request) {
        return Estimation.builder().type(BAILED).build();
    }

}
