package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;

import java.math.BigDecimal;

import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.HALF_UP;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class MidEstimator extends AbstractEstimator {

    @Override
    public Estimation estimate(Context context, Request request) {

        Key key = getKey(context, request);

        return estimate(context, key);

    }

    protected Estimation estimate(Context context, Key key) {

        BigDecimal ask = context.getBestAskPrice(key);

        if (ask == null) {
            return BAIL;
        }

        BigDecimal bid = context.getBestBidPrice(key);

        if (bid == null) {
            return BAIL;
        }

        BigDecimal mid = ask.add(bid).multiply(HALF);

        BigDecimal confidence = HALF;

        if (mid.signum() != 0) {

            BigDecimal spread = ask.subtract(bid).divide(mid, SCALE, HALF_UP).abs();

            confidence = ONE.subtract(spread).max(HALF);

        }

        log.debug("Estimated : {} (Confidence=[{} Ask=[{}] Bid=[{}]]", mid, confidence, ask, bid);

        return Estimation.builder().price(mid).confidence(confidence).build();

    }

}
