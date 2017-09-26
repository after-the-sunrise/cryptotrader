package com.after_sunrise.cryptocurrency.cryptotrader.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyType.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.DOWN;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.math.NumberUtils.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class PropertyManagerImpl implements PropertyController {

    private static final long INTERVAL_MIN = SECONDS.toMillis(1);

    private static final long INTERVAL_MAX = DAYS.toMillis(1);

    private static final String SEPARATOR_ENTRY = "|";

    private static final String SEPARATOR_KEYVAL = ":";

    private static final String KEY_TEMPLATE = "%s.%s.%s";

    private final Configuration configuration;

    private final Configuration override;

    @Inject
    public PropertyManagerImpl(Configuration configuration) {

        this.configuration = configuration;

        this.override = new BaseConfiguration();

    }

    @VisibleForTesting
    <T> T get(PropertyType type, String site, String instrument, BiFunction<Configuration, String, T> f) {

        if (StringUtils.isNotEmpty(site) && StringUtils.isNotEmpty(instrument)) {

            String specificKey = format(KEY_TEMPLATE, type.getKey(), site, instrument);

            if (override.containsKey(specificKey)) {
                return f.apply(override, specificKey);
            }

            if (configuration.containsKey(specificKey)) {
                return f.apply(configuration, specificKey);
            }

        }

        if (override.containsKey(type.getKey())) {
            return f.apply(override, type.getKey());
        }

        return f.apply(configuration, type.getKey());

    }

    @VisibleForTesting
    <I> void set(PropertyType type, String site, String instrument, I value, Function<I, ?> function) {

        String key;

        if (StringUtils.isNotEmpty(site) && StringUtils.isNotEmpty(instrument)) {
            key = format(KEY_TEMPLATE, type.getKey(), site, instrument);
        } else {
            key = type.getKey();
        }

        if (value == null) {

            override.clearProperty(key);

            log.info("Cleared override : {}", key);

        } else {

            Object newValue = function.apply(value);

            override.setProperty(key, newValue);

            log.info("Configured override : {}={}", key, newValue);

        }

    }

    @Override
    public Instant getNow() {
        return Instant.now();
    }

    @Override
    public String getVersion() {

        try {

            return get(VERSION, null, null, Configuration::getString);

        } catch (RuntimeException e) {

            log.warn("Invalid : " + VERSION, e);

            return StringUtils.EMPTY;

        }

    }

    @Override
    public Duration getTradingInterval() {

        try {

            long millis = get(TRADING_INTERVAL, null, null, Configuration::getLong);

            long adjusted = min(max(millis, INTERVAL_MIN), INTERVAL_MAX);

            log.trace("Fetched {} : {}ms -> {}ms", TRADING_INTERVAL, millis, adjusted);

            return Duration.ofMillis(adjusted);

        } catch (RuntimeException e) {

            log.warn("Invalid " + TRADING_INTERVAL, e);

            return Duration.ofMillis(INTERVAL_MAX);

        }

    }

    @Override
    public void setTradingInterval(Duration value) {
        set(TRADING_INTERVAL, null, null, value, Duration::toMillis);
    }

    @Override
    public Integer getTradingThreads() {

        try {

            Integer threads = get(TRADING_THREADS, null, null, Configuration::getInt);

            Integer adjusted = min(max(threads, INTEGER_ONE), Byte.MAX_VALUE);

            log.trace("Fetched {} : {}ms -> {}ms", TRADING_THREADS, threads, adjusted);

            return adjusted;

        } catch (RuntimeException e) {

            log.warn("Invalid " + TRADING_THREADS, e);

            return INTEGER_ONE;

        }

    }

    @Override
    public void setTradingThreads(Integer value) {
        set(TRADING_THREADS, null, null, value, Integer::valueOf);
    }

    @Override
    public Map<String, Set<String>> getTradingTargets() {

        String raw = null;

        try {

            raw = get(TRADING_TARGETS, null, null, Configuration::getString);

            Map<String, Set<String>> targets = new LinkedHashMap<>();

            for (String entry : split(trimToEmpty(raw), SEPARATOR_ENTRY)) {

                String[] kv = split(entry, SEPARATOR_KEYVAL, 2);

                if (kv.length != 2) {
                    continue;
                }

                targets.computeIfAbsent(kv[0], key -> new LinkedHashSet<>()).add(kv[1]);

            }

            log.trace("Fetched {} : {}", TRADING_TARGETS, targets);

            return Collections.unmodifiableMap(targets);

        } catch (RuntimeException e) {

            log.warn(format("Invalid %s : %s", TRADING_TARGETS, raw), e);

            return Collections.emptyMap();

        }

    }

    @Override
    public void setTradingTargets(Map<String, Set<String>> values) {
        set(TRADING_TARGETS, null, null, values, input -> StringUtils.join(
                input.entrySet().stream()
                        .filter(e -> StringUtils.isNotEmpty(e.getKey()))
                        .filter(e -> CollectionUtils.isNotEmpty(e.getValue()))
                        .map(entry -> StringUtils.join(entry.getValue().stream()
                                .filter(StringUtils::isNotEmpty)
                                .map(v -> entry.getKey() + SEPARATOR_KEYVAL + v)
                                .toArray(), SEPARATOR_ENTRY))
                        .toArray()
                , SEPARATOR_ENTRY));
    }

    @Override
    public Boolean getTradingActive(String site, String instrument) {

        try {

            boolean value = get(TRADING_ACTIVE, site, instrument, Configuration::getBoolean);

            log.trace("Fetched {} ({}.{}) : {}", TRADING_ACTIVE, site, instrument, value);

            return value;

        } catch (RuntimeException e) {

            log.warn(format("Invalid %s (%s.%s)", TRADING_ACTIVE, site, instrument), e);

            return false;

        }

    }

    @Override
    public void setTradingActive(String site, String instrument, Boolean value) {
        set(TRADING_ACTIVE, site, instrument, value, input -> input);
    }

    @Override
    public Integer getTradingFrequency(String site, String instrument) {

        try {

            Integer value = get(TRADING_FREQUENCY, site, instrument, Configuration::getInt);

            Integer adjusted = Math.max(value, INTEGER_ONE);

            log.trace("Fetched {} ({}.{}) : {} -> {}", TRADING_FREQUENCY, site, instrument, value, adjusted);

            return adjusted;

        } catch (RuntimeException e) {

            log.warn(format("Invalid %s (%s.%s)", TRADING_FREQUENCY, site, instrument), e);

            return INTEGER_ONE;

        }

    }

    @Override
    public void setTradingFrequency(String site, String instrument, Integer value) {
        set(TRADING_FREQUENCY, site, instrument, value, input -> input);
    }

    @Override
    public BigDecimal getTradingSpread(String site, String instrument) {

        try {

            BigDecimal value = get(TRADING_SPREAD, site, instrument, Configuration::getBigDecimal);

            BigDecimal adjusted = value.max(ZERO).min(ONE);

            log.trace("Fetched {} ({}.{}) : {} -> {}", TRADING_SPREAD, site, instrument, value, adjusted);

            return adjusted;

        } catch (RuntimeException e) {

            log.warn(format("Invalid %s (%s.%s)", TRADING_SPREAD, site, instrument), e);

            return ZERO;

        }

    }

    @Override
    public void setTradingSpread(String site, String instrument, BigDecimal value) {
        set(TRADING_SPREAD, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public BigDecimal getTradingSpreadAsk(String site, String instrument) {

        try {

            BigDecimal value = get(TRADING_SPREAD_ASK, site, instrument, Configuration::getBigDecimal);

            BigDecimal adjusted = value.max(ONE.negate()).min(ONE);

            log.trace("Fetched {} ({}.{}) : {} -> {}", TRADING_SPREAD_ASK, site, instrument, value, adjusted);

            return adjusted;

        } catch (RuntimeException e) {

            log.warn(format("Invalid %s (%s.%s)", TRADING_SPREAD_ASK, site, instrument), e);

            return ZERO;

        }

    }

    @Override
    public void setTradingSpreadAsk(String site, String instrument, BigDecimal value) {
        set(TRADING_SPREAD_ASK, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public BigDecimal getTradingSpreadBid(String site, String instrument) {

        try {

            BigDecimal value = get(TRADING_SPREAD_BID, site, instrument, Configuration::getBigDecimal);

            BigDecimal adjusted = value.max(ONE.negate()).min(ONE);

            log.trace("Fetched {} ({}.{}) : {} -> {}", TRADING_SPREAD_BID, site, instrument, value, adjusted);

            return adjusted;

        } catch (RuntimeException e) {

            log.warn(format("Invalid %s (%s.%s)", TRADING_SPREAD_BID, site, instrument), e);

            return ZERO;

        }

    }

    @Override
    public void setTradingSpreadBid(String site, String instrument, BigDecimal value) {
        set(TRADING_SPREAD_BID, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public BigDecimal getTradingExposure(String site, String instrument) {

        try {

            BigDecimal value = get(TRADING_EXPOSURE, site, instrument, Configuration::getBigDecimal);

            BigDecimal adjusted = value.max(ZERO).min(ONE);

            log.trace("Fetched {} ({}.{}) : {} -> {}", TRADING_EXPOSURE, site, instrument, value, adjusted);

            return adjusted;

        } catch (RuntimeException e) {

            log.warn(format("Invalid %s (%s.%s)", TRADING_EXPOSURE, site, instrument), e);

            return ZERO;

        }

    }

    @Override
    public void setTradingExposure(String site, String instrument, BigDecimal value) {
        set(TRADING_EXPOSURE, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public BigDecimal getTradingAversion(String site, String instrument) {

        try {

            BigDecimal value = get(TRADING_AVERSION, site, instrument, Configuration::getBigDecimal);

            BigDecimal adjusted = value.max(ZERO);

            log.trace("Fetched {} ({}.{}) : {} -> {}", TRADING_AVERSION, site, instrument, value, adjusted);

            return adjusted;

        } catch (RuntimeException e) {

            log.warn(format("Invalid %s (%s.%s)", TRADING_AVERSION, site, instrument), e);

            return ONE;

        }

    }

    @Override
    public void setTradingAversion(String site, String instrument, BigDecimal value) {
        set(TRADING_AVERSION, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public BigDecimal getTradingSplit(String site, String instrument) {

        try {

            BigDecimal value = get(TRADING_SPLIT, site, instrument, Configuration::getBigDecimal);

            BigDecimal adjusted = value.max(ONE).min(TEN).setScale(INTEGER_ZERO, DOWN);

            log.trace("Fetched {} ({}.{}) : {} -> {}", TRADING_SPLIT, site, instrument, value, adjusted);

            return adjusted;

        } catch (RuntimeException e) {

            log.warn(format("Invalid %s (%s.%s)", TRADING_SPLIT, site, instrument), e);

            return ONE;

        }

    }

    @Override
    public void setTradingSplit(String site, String instrument, BigDecimal value) {
        set(TRADING_SPLIT, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public Duration getTradingDuration(String site, String instrument) {

        try {

            Long value = get(TRADING_DURATION, site, instrument, Configuration::getLong);

            Duration adjusted = Duration.ofMillis(Math.max(value, LONG_ZERO));

            log.trace("Fetched {} ({}.{}) : {} -> {}", TRADING_DURATION, site, instrument, value, adjusted);

            return adjusted;

        } catch (RuntimeException e) {

            log.warn(format("Invalid %s (%s.%s)", TRADING_DURATION, site, instrument), e);

            return Duration.ZERO;

        }

    }

    @Override
    public void setTradingDuration(String site, String instrument, Duration value) {
        set(TRADING_DURATION, site, instrument, value, Duration::toMillis);
    }

    @Override
    public BigDecimal getFundingOffset(String site, String instrument) {

        try {

            BigDecimal value = get(FUNDING_OFFSET, site, instrument, Configuration::getBigDecimal);

            log.trace("Fetched {} ({}.{}) : {}", FUNDING_OFFSET, site, instrument, value);

            return value;

        } catch (RuntimeException e) {

            log.warn(format("Invalid %s (%s.%s)", FUNDING_OFFSET, site, instrument), e);

            return ZERO;

        }

    }

    @Override
    public void setFundingOffset(String site, String instrument, BigDecimal value) {
        set(FUNDING_OFFSET, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public Set<String> getEstimators(String site, String instrument) {

        try {

            String value = get(ESTIMATORS, site, instrument, Configuration::getString);

            String[] values = StringUtils.split(value, SEPARATOR_ENTRY);

            Set<String> ids = stream(values).filter(StringUtils::isNotEmpty).collect(toSet());

            log.trace("Fetched {} ({}.{}) : {}", ESTIMATORS, site, instrument, ids);

            return ids;

        } catch (RuntimeException e) {

            log.warn(format("Invalid %s (%s.%s)", ESTIMATORS, site, instrument), e);

            return emptySet();

        }

    }

    @Override
    public void setEstimators(String site, String instrument, Set<String> values) {
        set(ESTIMATORS, site, instrument, values, ids -> ofNullable(ids).orElse(emptySet())
                .stream().filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining(SEPARATOR_ENTRY)));
    }

}
