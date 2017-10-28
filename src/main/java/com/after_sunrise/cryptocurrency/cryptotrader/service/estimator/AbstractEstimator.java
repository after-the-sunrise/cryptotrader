package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.configuration2.ImmutableConfiguration;

import javax.inject.Inject;
import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public abstract class AbstractEstimator implements Estimator {

    protected static final Estimation BAIL = Estimation.builder().confidence(ZERO).build();

    private final String prefix = getClass().getName() + ".";

    private ImmutableConfiguration configuration;

    @Inject
    @VisibleForTesting
    public void setConfiguration(ImmutableConfiguration configuration) {
        this.configuration = configuration;
    }

    protected int getIntConfiguration(String key, int defaultValue) {

        int value;

        try {
            value = configuration.getInt(prefix + key, defaultValue);
        } catch (RuntimeException e) {
            value = defaultValue;
        }

        return value;

    }

    protected BigDecimal getDecimalConfiguration(String key, BigDecimal defaultValue) {

        BigDecimal value;

        try {
            value = configuration.getBigDecimal(prefix + key, defaultValue);
        } catch (RuntimeException e) {
            value = defaultValue;
        }

        return value;

    }

    protected String getStringConfiguration(String key, String defaultValue) {

        String value;

        try {
            value = configuration.getString(prefix + key, defaultValue);
        } catch (RuntimeException e) {
            value = defaultValue;
        }

        return value;

    }

    protected Key getKey(Request request) {
        return Key.from(request);
    }

}
