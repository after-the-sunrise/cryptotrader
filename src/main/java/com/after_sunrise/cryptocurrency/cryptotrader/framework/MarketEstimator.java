package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
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
public interface MarketEstimator {

    interface Context {
    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = PRIVATE)
    class Estimation {

        public enum Type {
            ESTIMATED, BAILED
        }

        private final Type type;

        private final BigDecimal price;

        private final BigDecimal confidence;

    }

    Estimation estimate(Context context, Request request);

}
