package com.after_sunrise.cryptocurrency.cryptotrader.core;

import lombok.Getter;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public enum PropertyType {

    VERSION,

    TRADING_TARGETS,

    TRADING_FREQUENCY,

    TRADING_INTERVAL,

    TRADING_THREADS,

    TRADING_SPREAD,

    TRADING_SPREAD_ASK,

    TRADING_SPREAD_BID,

    TRADING_SIGMA,

    TRADING_SAMPLES,

    TRADING_EXPOSURE,

    TRADING_THRESHOLD,

    TRADING_MINIMUM,

    TRADING_AVERSION,

    TRADING_SPLIT,

    TRADING_ACTIVE,

    TRADING_DURATION,

    FUNDING_OFFSET,

    FUNDING_MULTIPLIER_PRODUCTS,

    FUNDING_POSITIVE_MULTIPLIER,

    FUNDING_NEGATIVE_MULTIPLIER,

    FUNDING_POSITIVE_THRESHOLD,

    FUNDING_NEGATIVE_THRESHOLD,

    HEDGE_PRODUCTS,

    ESTIMATORS,

    ESTIMATOR_COMPOSITES,

    ESTIMATION_THRESHOLD;

    private static final String PREFIX = "cryptotrader.";

    @Getter
    private final String key;

    PropertyType() {
        this.key = PREFIX + name().toLowerCase();
    }

}
