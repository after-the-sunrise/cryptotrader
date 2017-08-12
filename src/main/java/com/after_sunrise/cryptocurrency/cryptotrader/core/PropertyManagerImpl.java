package com.after_sunrise.cryptocurrency.cryptotrader.core;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyType.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.math.BigDecimal.*;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.splitPreserveAllTokens;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class PropertyManagerImpl implements PropertyManager {

    private static final long INTERVAL_MIN = SECONDS.toMillis(1);

    private static final long INTERVAL_MAX = DAYS.toMillis(1);

    private static final String SEPARATOR_ENTRY = "|";

    private static final String SEPARATOR_KEYVAL = ":";

    private final Configuration configuration;

    @Inject
    public PropertyManagerImpl(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Instant getNow() {
        return Instant.now();
    }

    @Override
    public String getVersion() {

        try {

            return configuration.getString(VERSION.getKey());

        } catch (RuntimeException e) {

            log.warn("Invalid property : " + VERSION.getKey(), e);

            return StringUtils.EMPTY;

        }

    }

    @Override
    public Duration getTradingInterval() {

        try {

            long millis = configuration.getLong(TRADING_INTERVAL.getKey());

            long adjusted = min(max(millis, INTERVAL_MIN), INTERVAL_MAX);

            log.trace("Configured duration : {}ms -> {}ms", millis, adjusted);

            return Duration.ofMillis(adjusted);

        } catch (RuntimeException e) {

            log.warn("Invalid property : " + TRADING_INTERVAL.getKey(), e);

            return Duration.ofMillis(INTERVAL_MAX);

        }

    }

    @Override
    public Map<String, Set<String>> getTradingTargets() {

        String raw = null;

        try {

            raw = configuration.getString(TRADING_TARGETS.getKey());

            Map<String, Set<String>> targets = new LinkedHashMap<>();

            for (String entry : splitPreserveAllTokens(raw, SEPARATOR_ENTRY)) {

                String[] kv = splitPreserveAllTokens(entry, SEPARATOR_KEYVAL, 2);

                Set<String> instruments = targets.computeIfAbsent(kv[0], key -> new LinkedHashSet<>());

                instruments.add(kv[1]);

            }

            log.trace("Trading targets : {}", targets);

            return Collections.unmodifiableMap(targets);

        } catch (RuntimeException e) {

            String msg = "Invalid property : %s=%s";

            log.warn(String.format(msg, TRADING_TARGETS.getKey(), raw), e);

            return Collections.emptyMap();

        }

    }

    @Override
    public BigDecimal getTradingAggressiveness() {

        try {

            BigDecimal value = configuration.getBigDecimal(TRADING_AGGRESSIVENESS.getKey());

            BigDecimal adjusted = value.max(ZERO).min(TEN);

            log.trace("Configured aggressiveness : {} -> {}", value, adjusted);

            return adjusted;

        } catch (RuntimeException e) {

            log.warn("Invalid property : " + TRADING_AGGRESSIVENESS.getKey(), e);

            return ONE;

        }

    }

}
