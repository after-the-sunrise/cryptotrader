package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public abstract class TemplateContext implements Context {

    private static final long CACHE_SIZE = Byte.MAX_VALUE;

    private static final Duration CACHE_DURATION = Duration.ofSeconds(5);

    private final Map<Class<?>, Cache<Key, Optional<?>>> singleCache = new ConcurrentHashMap<>();

    private final Map<Class<?>, Cache<Key, Optional<List<?>>>> listCache = new ConcurrentHashMap<>();

    private final String id;

    private final CloseableHttpClient client;

    protected TemplateContext(String id) {

        this.id = id;

        this.client = HttpClients.createDefault();

    }

    @Override
    public String get() {
        return id;
    }

    @Override
    public void close() throws Exception {
        client.close();
    }

    @VisibleForTesting
    public String query(String path) throws IOException {
        return query(path, null);
    }

    @VisibleForTesting
    public String query(String path, Map<String, String> headers) throws IOException {

        ResponseHandler<String> handler = response -> {

            int status = response.getStatusLine().getStatusCode();

            if (HttpStatus.SC_OK != status) {

                log.trace("Query failure [{}] : status={}", path, status);

                return null;

            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            response.getEntity().writeTo(out);

            String data = new String(out.toByteArray(), StandardCharsets.UTF_8);

            log.trace("Queried [{}] : {}", path, data);

            return data;

        };

        HttpGet get = new HttpGet(path);

        Optional.ofNullable(headers).ifPresent(h -> h.forEach(get::setHeader));

        return client.execute(get, handler);

    }

    protected void clear() {

        log.trace("Clearing cache.");

        singleCache.forEach((k, v) -> v.invalidateAll());

        listCache.forEach((k, v) -> v.invalidateAll());

    }

    protected <T> T findCached(Class<T> type, Key key, Callable<T> c) {

        if (type == null || key == null) {
            return null;
        }

        Cache<Key, Optional<?>> cache = singleCache.computeIfAbsent(type, t -> createCache());

        try {

            Optional<?> cached = cache.get(key, () -> {

                T value = c.call();

                log.trace("Cached : {} - {}", key, value);

                return Optional.ofNullable(value);

            });

            return type.cast(cached.orElse(null));

        } catch (Exception e) {

            log.warn("Failed to cache : {} - {}", type, e);

            return null;

        }

    }

    protected <T> List<T> listCached(Class<T> type, Key key, Callable<List<T>> c) {

        if (type == null || key == null) {
            return emptyList();
        }

        Cache<Key, Optional<List<?>>> cache = listCache.computeIfAbsent(type, t -> createCache());

        try {

            Optional<List<?>> cached = cache.get(key, () -> {

                List<T> values = Optional.ofNullable(c.call()).orElse(emptyList());

                log.trace("Cached list : {} ({})", key, values.size());

                return Optional.of(values);

            });

            return cached.orElse(emptyList()).stream().map(type::cast).collect(Collectors.toList());

        } catch (Exception e) {

            log.warn("Failed to cache list : {} - {}", type, e);

            return emptyList();

        }

    }

    private <K0, K1 extends K0, V0, V1 extends V0> Cache<K1, V1> createCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(CACHE_SIZE)
                .expireAfterWrite(CACHE_DURATION.toMillis(), MILLISECONDS)
                .build();
    }

    protected <V> V extract(Future<V> future, Duration timeout) throws Exception {

        if (future == null) {
            return null;
        }

        try {

            return timeout == null ? future.get() : future.get(timeout.toMillis(), MILLISECONDS);

        } catch (TimeoutException e) {

            future.cancel(true);

            throw e;

        }

    }

    protected <V> V extractQuietly(Future<V> future, Duration timeout) {
        try {
            return extract(future, timeout);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public BigDecimal getBestAskPrice(Key key) {
        return null;
    }

    @Override
    public BigDecimal getBestBidPrice(Key key) {
        return null;
    }

    @Override
    public BigDecimal getMidPrice(Key key) {

        BigDecimal ask = getBestAskPrice(key);

        BigDecimal bid = getBestBidPrice(key);

        if (ask == null || bid == null) {
            return null;
        }

        return ask.add(bid).multiply(HALF);

    }

    @Override
    public BigDecimal getLastPrice(Key key) {
        return null;
    }

    @Override
    public List<Trade> listTrades(Key key, Instant fromTime) {
        return null;
    }

    @Override
    public BigDecimal getInstrumentPosition(Key key) {
        return null;
    }

    @Override
    public BigDecimal getFundingPosition(Key key) {
        return null;
    }

    @Override
    public BigDecimal roundLotSize(Key key, BigDecimal value, RoundingMode mode) {
        return null;
    }

    @Override
    public BigDecimal roundTickSize(Key key, BigDecimal value, RoundingMode mode) {
        return null;
    }

    @Override
    public BigDecimal getCommissionRate(Key key) {
        return null;
    }

    @Override
    public Boolean isMarginable(Key key) {
        return null;
    }

    @Override
    public ZonedDateTime getExpiry(Key key) {
        return null;
    }

    @Override
    public Order findOrder(Key key, String id) {
        return null;
    }

    @Override
    public List<Order> listActiveOrders(Key key) {
        return null;
    }

    @Override
    public List<Order.Execution> listExecutions(Key key) {
        return null;
    }

    @Override
    public Map<CreateInstruction, String> createOrders(Key key, Set<CreateInstruction> instructions) {
        return null;
    }

    @Override
    public Map<CancelInstruction, String> cancelOrders(Key key, Set<CancelInstruction> instructions) {
        return null;
    }

}
