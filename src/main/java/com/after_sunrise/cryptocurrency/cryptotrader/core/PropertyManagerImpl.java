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
import static java.lang.String.format;
import static java.math.BigDecimal.*;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class PropertyManagerImpl implements PropertyController {

    private static final BigDecimal INTERVAL_MIN = BigDecimal.valueOf(SECONDS.toMillis(1));

    private static final BigDecimal INTERVAL_MAX = BigDecimal.valueOf(DAYS.toMillis(1));

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

    @VisibleForTesting
    String getString(String site, String instrument, PropertyType type, String defaultValue) {

        try {

            String value = get(type, site, instrument, Configuration::getString);

            String adjusted = Objects.toString(value, defaultValue);

            log.trace("Fetched {} ({}.{}) : {} -> {}", type, site, instrument, value, adjusted);

            return adjusted;

        } catch (RuntimeException e) {

            log.warn(format("Invalid %s (%s.%s)", type, site, instrument), e);

            return defaultValue;

        }

    }

    @VisibleForTesting
    BigDecimal getDecimal(String site, String instrument,
                          PropertyType type, BigDecimal min, BigDecimal max, BigDecimal defaultValue) {

        try {

            BigDecimal value = get(type, site, instrument, Configuration::getBigDecimal);

            BigDecimal adjusted = value;

            if (min != null) {
                adjusted = adjusted.max(min);
            }

            if (max != null) {
                adjusted = adjusted.min(max);
            }

            log.trace("Fetched {} ({}.{}) : {} -> {}", type, site, instrument, value, adjusted);

            return adjusted;

        } catch (RuntimeException e) {

            log.warn(format("Invalid %s (%s.%s)", type, site, instrument), e);

            return defaultValue;

        }

    }

    @VisibleForTesting
    Map<String, Set<String>> getProducts(String site, String instrument, PropertyType type) {

        String raw = null;

        try {

            raw = get(type, site, instrument, Configuration::getString);

            Map<String, Set<String>> targets = new LinkedHashMap<>();

            for (String entry : split(trimToEmpty(raw), SEPARATOR_ENTRY)) {

                String[] kv = split(entry, SEPARATOR_KEYVAL, 2);

                if (kv.length != 2) {
                    continue;
                }

                targets.computeIfAbsent(kv[0], key -> new LinkedHashSet<>()).add(kv[1]);

            }

            log.trace("Fetched {} : {}", type, targets);

            return Collections.unmodifiableMap(targets);

        } catch (RuntimeException e) {

            log.warn(format("Invalid %s : %s", type, raw), e);

            return Collections.emptyMap();

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

        BigDecimal value = getDecimal(null, null, TRADING_INTERVAL, INTERVAL_MIN, INTERVAL_MAX, INTERVAL_MAX);

        return Duration.ofMillis(value.longValue());

    }

    @Override
    public void setTradingInterval(Duration value) {
        set(TRADING_INTERVAL, null, null, value, Duration::toMillis);
    }

    @Override
    public Integer getTradingThreads() {
        return getDecimal(null, null, TRADING_THREADS,
                ONE, BigDecimal.valueOf(Byte.MAX_VALUE), ONE).intValue();
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
        return getDecimal(site, instrument, TRADING_FREQUENCY, ONE, null, ONE).intValue();
    }

    @Override
    public void setTradingFrequency(String site, String instrument, Integer value) {
        set(TRADING_FREQUENCY, site, instrument, value, input -> input);
    }

    @Override
    public BigDecimal getTradingSpread(String site, String instrument) {
        return getDecimal(site, instrument, TRADING_SPREAD, ZERO, ONE, ZERO);
    }

    @Override
    public void setTradingSpread(String site, String instrument, BigDecimal value) {
        set(TRADING_SPREAD, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public BigDecimal getTradingSpreadAsk(String site, String instrument) {
        return getDecimal(site, instrument, TRADING_SPREAD_ASK, ONE.negate(), ONE, ZERO);
    }

    @Override
    public void setTradingSpreadAsk(String site, String instrument, BigDecimal value) {
        set(TRADING_SPREAD_ASK, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public BigDecimal getTradingSpreadBid(String site, String instrument) {
        return getDecimal(site, instrument, TRADING_SPREAD_BID, ONE.negate(), ONE, ZERO);
    }

    @Override
    public void setTradingSpreadBid(String site, String instrument, BigDecimal value) {
        set(TRADING_SPREAD_BID, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public BigDecimal getTradingSigma(String site, String instrument) {
        return getDecimal(site, instrument, TRADING_SIGMA, ZERO, null, ZERO);
    }

    @Override
    public void setTradingSigma(String site, String instrument, BigDecimal value) {
        set(TRADING_SIGMA, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public Integer getTradingSamples(String site, String instrument) {
        return getDecimal(site, instrument, TRADING_SAMPLES, ZERO, null, ZERO).intValue();
    }

    @Override
    public void setTradingSamples(String site, String instrument, Integer value) {
        set(TRADING_SAMPLES, site, instrument, value, input -> input);
    }

    @Override
    public BigDecimal getTradingExposure(String site, String instrument) {
        return getDecimal(site, instrument, TRADING_EXPOSURE, ZERO, ONE, ZERO);
    }

    @Override
    public void setTradingExposure(String site, String instrument, BigDecimal value) {
        set(TRADING_EXPOSURE, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public BigDecimal getTradingThreshold(String site, String instrument) {
        return getDecimal(site, instrument, TRADING_THRESHOLD, ZERO, null, ZERO);
    }

    @Override
    public void setTradingThreshold(String site, String instrument, BigDecimal value) {
        set(TRADING_THRESHOLD, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public BigDecimal getTradingMinimum(String site, String instrument) {
        return getDecimal(site, instrument, TRADING_MINIMUM, ZERO, null, ZERO);
    }

    @Override
    public void setTradingMinimum(String site, String instrument, BigDecimal value) {
        set(TRADING_MINIMUM, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public BigDecimal getTradingAversion(String site, String instrument) {
        return getDecimal(site, instrument, TRADING_AVERSION, ZERO, null, ONE);
    }

    @Override
    public void setTradingAversion(String site, String instrument, BigDecimal value) {
        set(TRADING_AVERSION, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public String getTradingInstruction(String site, String instrument) {
        return getString(site, instrument, TRADING_INSTRUCTION, StringUtils.EMPTY);
    }

    @Override
    public void setTradingInstruction(String site, String instrument, String value) {
        set(TRADING_INSTRUCTION, site, instrument, value, Function.identity());
    }

    @Override
    public Integer getTradingSplit(String site, String instrument) {
        return getDecimal(site, instrument, TRADING_SPLIT, ONE, TEN, ONE).intValue();
    }

    @Override
    public void setTradingSplit(String site, String instrument, Integer value) {
        set(TRADING_SPLIT, site, instrument, value, input -> input);
    }

    @Override
    public Duration getTradingDuration(String site, String instrument) {

        BigDecimal value = getDecimal(site, instrument, TRADING_DURATION, null, null, ZERO);

        return Duration.ofMillis(value.longValue());

    }

    @Override
    public void setTradingDuration(String site, String instrument, Duration value) {
        set(TRADING_DURATION, site, instrument, value, Duration::toMillis);
    }

    @Override
    public BigDecimal getFundingOffset(String site, String instrument) {
        return getDecimal(site, instrument, FUNDING_OFFSET, null, null, ZERO);
    }

    @Override
    public void setFundingOffset(String site, String instrument, BigDecimal value) {
        set(FUNDING_OFFSET, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public Map<String, Set<String>> getFundingMultiplierProducts(String site, String instrument) {
        return getProducts(site, instrument, FUNDING_MULTIPLIER_PRODUCTS);
    }

    @Override
    public void setFundingMultiplierProducts(String site, String instrument, Map<String, Set<String>> values) {
        set(FUNDING_MULTIPLIER_PRODUCTS, site, instrument, values, input -> StringUtils.join(
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
    public BigDecimal getFundingPositiveMultiplier(String site, String instrument) {
        return getDecimal(site, instrument, FUNDING_POSITIVE_MULTIPLIER, null, null, ONE);
    }

    @Override
    public void setFundingPositiveMultiplier(String site, String instrument, BigDecimal value) {
        set(FUNDING_POSITIVE_MULTIPLIER, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public BigDecimal getFundingNegativeMultiplier(String site, String instrument) {
        return getDecimal(site, instrument, FUNDING_NEGATIVE_MULTIPLIER, null, null, ONE);
    }

    @Override
    public void setFundingNegativeMultiplier(String site, String instrument, BigDecimal value) {
        set(FUNDING_NEGATIVE_MULTIPLIER, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public BigDecimal getFundingPositiveThreshold(String site, String instrument) {
        return getDecimal(site, instrument, FUNDING_POSITIVE_THRESHOLD, null, null, ONE);
    }

    @Override
    public void setFundingPositiveThreshold(String site, String instrument, BigDecimal value) {
        set(FUNDING_POSITIVE_THRESHOLD, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public BigDecimal getFundingNegativeThreshold(String site, String instrument) {
        return getDecimal(site, instrument, FUNDING_NEGATIVE_THRESHOLD, null, null, ONE);
    }

    @Override
    public void setFundingNegativeThreshold(String site, String instrument, BigDecimal value) {
        set(FUNDING_NEGATIVE_THRESHOLD, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public Map<String, Set<String>> getHedgeProducts(String site, String instrument) {
        return getProducts(site, instrument, HEDGE_PRODUCTS);
    }

    @Override
    public void setHedgeProducts(String site, String instrument, Map<String, Set<String>> values) {
        set(HEDGE_PRODUCTS, site, instrument, values, input -> StringUtils.join(
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

    @Override
    public Map<String, Set<String>> getEstimatorComposites(String site, String instrument) {
        return getProducts(site, instrument, ESTIMATOR_COMPOSITES);
    }

    @Override
    public void setEstimatorComposites(String site, String instrument, Map<String, Set<String>> values) {
        set(ESTIMATOR_COMPOSITES, site, instrument, values, input -> StringUtils.join(
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
    public BigDecimal getEstimationThreshold(String site, String instrument) {
        return getDecimal(site, instrument, ESTIMATION_THRESHOLD, ZERO, ONE, ZERO);
    }

    @Override
    public void setEstimationThreshold(String site, String instrument, BigDecimal value) {
        set(ESTIMATION_THRESHOLD, site, instrument, value, BigDecimal::toPlainString);
    }

    @Override
    public BigDecimal getEstimationAversion(String site, String instrument) {
        return getDecimal(site, instrument, ESTIMATION_AVERSION, ZERO, null, ZERO);
    }

    @Override
    public void setEstimationAversion(String site, String instrument, BigDecimal value) {
        set(ESTIMATION_AVERSION, site, instrument, value, BigDecimal::toPlainString);
    }

}
