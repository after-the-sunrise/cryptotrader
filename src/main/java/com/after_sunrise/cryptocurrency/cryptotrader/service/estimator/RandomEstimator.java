package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

import static java.math.BigDecimal.ONE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class RandomEstimator extends AbstractEstimator {

    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    private static final int POINTS = 2;

    @Override
    public Estimation estimate(Context context, Request request) {

        BigDecimal last = context.getLastPrice(getKey(request));

        if (last == null) {
            return BAIL;
        }

        // +1 ~ -1
        BigDecimal random = BigDecimal.valueOf(RANDOM.nextDouble()).subtract(HALF).divide(HALF);

        // Less confident if randomness is larger.
        BigDecimal confidence = ONE.subtract(random.abs()).pow(POINTS);

        // 1.01% ~ 0.99%
        BigDecimal price = last.multiply(ONE.add(random.movePointLeft(POINTS)));

        log.debug("Estimated : {} (confidence=[{}])", price, confidence);

        return Estimation.builder().price(price).confidence(confidence).build();

    }

}
