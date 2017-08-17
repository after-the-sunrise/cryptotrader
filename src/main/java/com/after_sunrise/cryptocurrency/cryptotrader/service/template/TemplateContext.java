package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.core.Environment;
import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.math.NumberUtils.LONG_ONE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public abstract class TemplateContext implements Context {

    private final Map<Class<?>, Cache<Instant, Optional<?>>> singleCache = new ConcurrentHashMap<>();

    private final Map<Class<?>, Cache<Instant, Optional<? extends List<?>>>> listCache = new ConcurrentHashMap<>();

    private final String id;

    private Environment environment;

    public TemplateContext(String id) {
        this.id = id;
    }

    @Override
    public String get() {
        return id;
    }

    @Inject
    public void init(Injector injector) {

        environment = injector.getInstance(PropertyManager.class);

        initialize(injector);

        log.debug("Initialized.");

    }

    protected abstract void initialize(Injector injector);

    @VisibleForTesting
    Instant calculateKey(Key key, Duration interval) {

        if (!Key.isValid(key)) {
            return null;
        }

        long now = key.getTimestamp().toEpochMilli();

        long unit = Math.max(interval.toMillis(), LONG_ONE);

        long rounded = (now / unit + LONG_ONE) * unit;

        return Instant.ofEpochMilli(rounded);

    }

    protected <T> T findCached(Class<T> cacheType, Key key, Duration interval, Callable<T> c) {

        Instant cacheKey = calculateKey(key, interval);

        if (cacheType == null || cacheKey == null) {
            return null;
        }

        Cache<Instant, Optional<?>> cache = singleCache.computeIfAbsent(cacheType, t ->
                CacheBuilder.newBuilder().maximumSize(10).build()
        );

        try {

            Optional<?> cached = cache.get(cacheKey, () -> Optional.ofNullable(c.call()));

            return cacheType.cast(cached.orElse(null));

        } catch (Exception e) {

            log.warn("Failed to cache : {} - {}", cacheType, e);

            return null;

        }

    }

    protected <T> List<T> listCached(Class<T> cacheType, Key key, Duration interval, Callable<List<T>> c) {

        Instant cacheKey = calculateKey(key, interval);

        if (cacheType == null || cacheKey == null) {
            return Collections.emptyList();
        }

        Cache<Instant, Optional<? extends List<?>>> cache = listCache.computeIfAbsent(cacheType, t ->
                CacheBuilder.newBuilder().maximumSize(10).build()
        );

        try {

            Optional<? extends List<?>> cached = cache.get(cacheKey, () -> Optional.of(c.call()));

            if (!cached.isPresent()) {
                return Collections.emptyList();
            }

            return cached.get().stream().map(cacheType::cast).collect(Collectors.toList());

        } catch (Exception e) {

            log.warn("Failed to cache list : {} - {}", cacheType, e);

            return Collections.emptyList();

        }

    }

}
