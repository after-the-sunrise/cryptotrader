package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.MarketEstimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.PortfolioAdviser;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import lombok.extern.slf4j.Slf4j;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class PortfolioAdviserImpl implements PortfolioAdviser {

    @Override
    public Advice advise(Context context, Request request, Estimation estimation) {

        return Advice.builder().build();

    }

}
