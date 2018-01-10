package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;

import java.math.BigDecimal;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class MicroEstimator extends AbstractEstimator {

    @Override
    public Estimation estimate(Context context, Request request) {

        Key key = getKey(context, request);

        return estimate(context, key);

    }

    protected Estimation estimate(Context context, Key key) {

        BigDecimal askSize = context.getBestAskSize(key);

        BigDecimal bidSize = context.getBestBidSize(key);

        if (askSize == null || bidSize == null) {
            return BAIL;
        }

        BigDecimal totalSize = askSize.add(bidSize);

        if (totalSize.signum() == 0) {
            return BAIL;
        }

        BigDecimal ask = context.getBestAskPrice(key);

        BigDecimal bid = context.getBestBidPrice(key);

        if (ask == null || bid == null) {
            return BAIL;
        }

        BigDecimal askVolume = ask.multiply(askSize);

        BigDecimal bidVolume = bid.multiply(bidSize);

        BigDecimal average = askVolume.add(bidVolume).divide(totalSize, SCALE, HALF_UP);

        BigDecimal confidence = HALF;

        if (average.signum() != 0) {

            BigDecimal spread = ask.subtract(bid).divide(average, SCALE, HALF_UP).abs();

            confidence = ONE.subtract(spread).multiply(HALF).min(ONE).max(ZERO);

        }

        log.debug("Estimated : {} (Confidence=[{} Ask=[{}] Bid=[{}]]", average, confidence, ask, bid);

        return Estimation.builder().price(average).confidence(confidence).build();

    }

}
