package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.Supplier;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface MarketEstimator {

    interface Context extends Supplier<String> {

        BigDecimal getBesAskPrice(String site, String instrument, Instant timestamp);

        BigDecimal getBesBidPrice(String site, String instrument, Instant timestamp);

    }

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
