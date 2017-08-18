package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public abstract class TemplateContext implements Context {

    private static final long SIZE = 3;

    private static final long EXPIRY = SECONDS.toMillis(SIZE);

    private final Map<Class<?>, Cache<Key, Optional<?>>> singleCache = new ConcurrentHashMap<>();

    private final Map<Class<?>, Cache<Key, Optional<? extends List<?>>>> listCache = new ConcurrentHashMap<>();

    private final String id;

    public TemplateContext(String id) {
        this.id = id;
    }

    @Override
    public String get() {
        return id;
    }

    @VisibleForTesting
    <K0, K1 extends K0, V0, V1 extends V0> Cache<K1, V1> createCache(Class<?> clazz) {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(EXPIRY, MILLISECONDS)
                .expireAfterAccess(EXPIRY, MILLISECONDS)
                .maximumSize(SIZE)
                .build();
    }

    protected <T> T findCached(Class<T> type, Key key, Callable<T> c) {

        if (type == null || key == null) {
            return null;
        }

        Cache<Key, Optional<?>> cache = singleCache.computeIfAbsent(type, this::createCache);

        try {

            Optional<?> cached = cache.get(key, () -> Optional.ofNullable(c.call()));

            return type.cast(cached.orElse(null));

        } catch (Exception e) {

            log.warn("Failed to cache : {} - {}", type, e);

            return null;

        }

    }

    protected <T> List<T> listCached(Class<T> type, Key key, Callable<List<T>> c) {

        if (type == null || key == null) {
            return null;
        }

        Cache<Key, Optional<? extends List<?>>> cache = listCache.computeIfAbsent(type, this::createCache);

        try {

            Optional<? extends List<?>> cached = cache.get(key, () -> Optional.of(c.call()));

            if (!cached.isPresent()) {
                return Collections.emptyList();
            }

            return cached.get().stream().map(type::cast).collect(Collectors.toList());

        } catch (Exception e) {

            log.warn("Failed to cache list : {} - {}", type, e);

            return Collections.emptyList();

        }

    }

}
