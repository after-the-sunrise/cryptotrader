package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class RandomEstimator implements Estimator {

    private static final Estimation BAIL = Estimation.builder().confidence(ZERO).build();

    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    private static final BigDecimal CENTER = new BigDecimal("0.5");

    private static final int POINTS = 2;

    @Override
    public String get() {
        return getClass().getSimpleName();
    }

    @Override
    public Estimation estimate(Context context, Request request) {

        BigDecimal last = context.getLastPrice(Key.from(request));

        if (last == null) {
            return BAIL;
        }

        // +1 ~ -1
        BigDecimal random = BigDecimal.valueOf(RANDOM.nextDouble()).subtract(CENTER).divide(CENTER);

        // Less confident if randomness is larger.
        BigDecimal confidence = ONE.subtract(random.abs()).pow(POINTS);

        // 1.01% ~ 0.99%
        BigDecimal price = last.multiply(ONE.add(random.movePointLeft(POINTS)));

        log.debug("Estimated : {} (confidence=[{}])", price, confidence);

        return Estimation.builder().price(price).confidence(confidence).build();

    }

}
