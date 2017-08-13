package com.after_sunrise.cryptocurrency.cryptotrader.service.strategy;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.MarketEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.impl.Frameworks;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

import static java.math.BigDecimal.ONE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class LastMarketEstimator implements MarketEstimator {

    private static final Estimation BAIL = Estimation.builder().build();

    @Override
    public Estimation estimate(Context context, Request request) {

        Key key = Frameworks.convert(request);

        if (key == null || context == null) {
            return BAIL;
        }

        BigDecimal price = context.getLastPrice(key);

        Estimation estimation = Estimation.builder().price(price).confidence(ONE).build();

        log.debug("Estimated : {} - {}", estimation, key);

        return estimation;

    }

}
