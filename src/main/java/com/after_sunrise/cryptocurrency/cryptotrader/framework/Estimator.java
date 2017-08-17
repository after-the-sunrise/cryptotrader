package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.function.Supplier;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface Estimator extends Supplier<String> {

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = PRIVATE)
    class Estimation {

        private final BigDecimal price;

        private final BigDecimal confidence;

    }

    Estimation estimate(Context context, Request request);

}
