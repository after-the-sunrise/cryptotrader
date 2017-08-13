package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.google.inject.Injector;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Supplier;

import static java.math.BigDecimal.ZERO;
import static lombok.AccessLevel.PRIVATE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
@NoArgsConstructor(access = PRIVATE)
public class Frameworks {

    public static <T> List<T> load(Class<T> clazz, Injector injector) {

        log.info("Loading service list : {}", clazz);

        ServiceLoader<T> loader = ServiceLoader.load(clazz);

        Iterator<T> itr = loader.iterator();

        List<T> services = new ArrayList<>();

        while (itr.hasNext()) {

            try {

                T service = itr.next();

                injector.injectMembers(service);

                services.add(service);

                log.info("Loaded service : {}", service);

            } catch (RuntimeException e) {

                log.warn("Skipped service.", e);

            }

        }

        return Collections.unmodifiableList(services);

    }

    public static <K, T extends Supplier<K>> Map<K, T> loadMap(Class<T> clazz, Injector injector) {

        log.info("Loading service map : {}", clazz);

        ServiceLoader<T> loader = ServiceLoader.load(clazz);

        Iterator<T> itr = loader.iterator();

        Map<K, T> services = new LinkedHashMap<>();

        while (itr.hasNext()) {

            try {

                T service = itr.next();

                injector.injectMembers(service);

                services.put(service.get(), service);

                log.info("Loaded service : {} - {}", service.get(), service);

            } catch (ServiceConfigurationError | RuntimeException e) {

                log.warn("Skipped service.", e);

            }

        }

        return Collections.unmodifiableMap(services);

    }

    public static boolean isInvalid(Request value) {

        if (value == null) {
            return true;
        }

        if (StringUtils.isEmpty(value.getSite())) {
            return true;
        }

        if (StringUtils.isEmpty(value.getInstrument())) {
            return true;
        }

        if (value.getTimestamp() == null) {
            return true;
        }

        if (value.getAggressiveness() == null) {
            return true;
        }

        return false;

    }

    public static boolean isInvalid(Key value) {

        if (value == null) {
            return true;
        }

        if (StringUtils.isEmpty(value.getSite())) {
            return true;
        }

        if (StringUtils.isEmpty(value.getInstrument())) {
            return true;
        }

        if (value.getTimestamp() == null) {
            return true;
        }

        return false;

    }

    public static Key convert(Trader.Request request) {

        Key.KeyBuilder builder = Key.builder();

        if (request == null) {
            return builder.build();
        }

        builder = builder.site(request.getSite());

        builder = builder.instrument(request.getInstrument());

        builder = builder.timestamp(request.getTimestamp());

        return builder.build();

    }

    public static BigDecimal trimToZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    public static <T> List<T> trimToEmpty(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

}
