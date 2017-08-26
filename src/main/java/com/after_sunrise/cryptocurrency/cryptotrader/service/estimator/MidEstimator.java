package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;

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

        log.debug("Estimated : {} (Ask=[{}] Bid=[{}] Confidence=[{}]", mid, ask, bid, confidence);

        return Estimation.builder().price(mid).confidence(confidence).build();

    }

}
