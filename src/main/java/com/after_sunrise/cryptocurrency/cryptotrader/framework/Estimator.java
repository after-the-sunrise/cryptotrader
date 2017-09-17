package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface Estimator extends Service {

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = PRIVATE)
    class Estimation {

        private final BigDecimal price;

        private final BigDecimal confidence;

    }

    @Override
    default String get() {
        return getClass().getSimpleName();
    }

    Estimation estimate(Context context, Request request);

}
