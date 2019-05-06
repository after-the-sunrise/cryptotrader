package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.core.Converter;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.impl.AbstractService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Futures;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public abstract class TemplateContext extends AbstractService implements Context {

    public enum RequestType {

        GET(HttpGet::new),

        PUT(HttpPut::new),

        POST(HttpPost::new),

        DELETE(url -> new HttpPost(url) { // Payload Support
            @Override
            public String getMethod() {
                return DELETE.name();
            }
        });

        private final Function<String, HttpRequestBase> delegate;

        RequestType(Function<String, HttpRequestBase> function) {

            this.delegate = function;

        }

        public HttpRequestBase create(String url, Map<String, String> headers, String data) {

            HttpRequestBase request = delegate.apply(url);

            Optional.ofNullable(headers).orElse(emptyMap()).forEach(request::setHeader);

            Optional.ofNullable(data)
                    .filter(StringUtils::isNotEmpty)
                    .filter(d -> HttpEntityEnclosingRequest.class.isInstance(request))
                    .map(d -> new StringEntity(data, UTF_8))
                    .ifPresent(d -> HttpEntityEnclosingRequest.class.cast(request).setEntity(d));

            return request;

        }

    }

    private static final Logger LOG = LoggerFactory.getLogger(RequestType.class);

    private static final long CACHE_SIZE = Byte.MAX_VALUE;

    private static final Duration CACHE_DURATION = Duration.ofMinutes(3);

    private static final Duration CACHE_SLEEP = Duration.ofMillis(100);

    private static final int CACHE_RETRY = 2;

    private static final Duration CLIENT_TIMEOUT = Duration.ofMinutes(3);

    private static final Duration FUTURE_TIMEOUT = Duration.ofSeconds(30);

    private static final Duration FUTURE_MINIMUM = Duration.ofMillis(100);

    private final Map<Class<?>, Cache<Key, Optional<?>>> singleCache = synchronizedMap(new HashMap<>());

    private final Map<Class<?>, Cache<Key, Optional<List<?>>>> listCache = synchronizedMap(new HashMap<>());

    private final Map<Pair<Class<?>, Key>, Optional<?>> singleLast = new ConcurrentHashMap<>();

    private final Map<Pair<Class<?>, Key>, Optional<List<?>>> listLast = new ConcurrentHashMap<>();

    private final String id;

    private final ExecutorService executor;

    private final CloseableHttpClient client;

    private final AtomicReference<StateType> state;

    protected TemplateContext(String id) {

        this.id = id;

        this.executor = Executors.newCachedThreadPool();

        this.state = new AtomicReference<>(StateType.ACTIVE);

        this.client = HttpClients.custom()
                .evictExpiredConnections()
                .evictIdleConnections(CLIENT_TIMEOUT.toMillis(), MILLISECONDS)
                .setConnectionTimeToLive(CLIENT_TIMEOUT.toMillis(), MILLISECONDS)
                .setMaxConnPerRoute(Byte.MAX_VALUE)
                .setMaxConnTotal(Byte.MAX_VALUE)
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                                .setCookieSpec(CookieSpecs.STANDARD)
                                .setSocketTimeout((int) CLIENT_TIMEOUT.toMillis())
                                .setConnectTimeout((int) CLIENT_TIMEOUT.toMillis())
                                .setConnectionRequestTimeout((int) CLIENT_TIMEOUT.toMillis())
                                .build()
                )
                .build();


    }

    @Override
    public String get() {
        return id;
    }

    @Override
    public void close() throws Exception {

        state.set(StateType.TERMINATE);

        executor.shutdown();

        client.close();

    }

    @VisibleForTesting
    public Instant getNow() {
        return Instant.now();
    }

    @VisibleForTesting
    public String getUniqueId() {
        return UUID.randomUUID().toString();
    }

    @VisibleForTesting
    public String computeHash(String algorithm, byte[] key, byte[] data) throws IOException {

        try {

            Mac mac = Mac.getInstance(algorithm);

            mac.init(new SecretKeySpec(key, algorithm));

            byte[] hash = mac.doFinal(data);

            StringBuilder sb = new StringBuilder(hash.length);

            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }

            return sb.toString();

        } catch (GeneralSecurityException e) {

            throw new IOException("Failed to compute hash.", e);

        }

    }

    @VisibleForTesting
    public String buildQueryParameter(Map<String, String> parameters) throws IOException {
        return buildQueryParameter(parameters, "?");
    }

    @VisibleForTesting
    public String buildQueryParameter(Map<String, String> parameters, String prefix) throws IOException {

        StringBuilder sb = new StringBuilder();

        if (MapUtils.isNotEmpty(parameters)) {

            for (Entry<String, String> entry : parameters.entrySet()) {

                if (StringUtils.isEmpty(entry.getKey())) {
                    continue;
                }

                if (StringUtils.isEmpty(entry.getValue())) {
                    continue;
                }

                sb.append(sb.length() == 0 ? StringUtils.trimToEmpty(prefix) : "&");
                sb.append(URLEncoder.encode(entry.getKey(), UTF_8.name()));
                sb.append('=');
                sb.append(URLEncoder.encode(entry.getValue(), UTF_8.name()));

            }

        }

        return sb.toString();

    }

    @VisibleForTesting
    public String request(String path) throws IOException {
        return request(RequestType.GET, path, null, null);
    }

    @VisibleForTesting
    public String request(RequestType type, String path, Map<String, String> headers, String data) throws IOException {

        LOG.trace("[SEND][{}][{}][{}] {}", type, path, headers, data);

        Instant start = Instant.now();

        HttpRequestBase request = type.create(path, headers, data);

        int t = (int) getTimeout().toMillis();

        request.setConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD)
                .setConnectTimeout(t).setConnectionRequestTimeout(t).setSocketTimeout(t).build()
        );

        return client.execute(request, response -> {

            String body = EntityUtils.toString(response.getEntity(), UTF_8);

            StatusLine statusLine = response.getStatusLine();

            Duration elapsed = Duration.between(start, Instant.now());

            LOG.trace("[RECV][{}][{}][{}ms][{}] {}",
                    path, statusLine, elapsed.toMillis(), response.getAllHeaders(), body);

            if (HttpStatus.SC_OK != statusLine.getStatusCode()) {

                String trimmed = body.replaceAll("[\r\n]", "");

                throw new IOException(statusLine + " : " + trimmed);

            }

            return body;

        });

    }

    @VisibleForTesting
    public Future<String> requestAsync(RequestType type, String path, Map<String, String> headers, String data) {

        LOG.trace("[SEND][{}][{}][{}] {}", type, path, headers, data);

        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            executor.execute(() -> {
                try {
                    future.complete(request(type, path, headers, data));
                } catch (IOException e) {
                    future.completeExceptionally(e);
                }
            });
        } catch (RejectedExecutionException e) {
            future.completeExceptionally(e);
        }

        return future;

    }

    @VisibleForTesting
    public void clear() {

        log.trace("Clearing cache.");

        singleCache.forEach((k, v) -> v.invalidateAll());

        listCache.forEach((k, v) -> v.invalidateAll());

        singleLast.clear();

        listLast.clear();

    }

    protected <T> T findCached(Class<T> type, Key key, Callable<T> c) {
        return findCached(type, key, c, true);
    }

    protected <T> T findCached(Class<T> type, Key key, Callable<T> c, boolean cacheLast) {

        if (type == null || key == null || c == null) {
            return null;
        }

        Pair lastKey = Pair.of(type, Key.build(key).timestamp(null).build());

        Cache<Key, Optional<?>> cache = singleCache.computeIfAbsent(type, this::createCache);

        synchronized (cache) {

            int retry = 0;

            try {

                while (true) {

                    try {

                        Optional<?> cached = cache.get(key, () -> {

                            T value = c.call();

                            log.trace("Cached : {} - {}", key, value);

                            return Optional.ofNullable(value);

                        });

                        if (cacheLast) {
                            singleLast.put(lastKey, cached);
                        }

                        return cached.map(type::cast).orElse(null);

                    } catch (Exception e) {

                        if (CACHE_RETRY < ++retry) {

                            log.warn("Failed to cache : {} - {}", type, e);

                            if (cacheLast) {
                                cache.put(key, singleLast.getOrDefault(lastKey, Optional.empty()));
                            } else {
                                cache.put(key, Optional.empty());
                            }

                            break;

                        }

                        Thread.sleep(CACHE_SLEEP.toMillis());

                    }

                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt(); // Propagate interruption.

            }

        }

        Optional<?> last = singleLast.getOrDefault(lastKey, Optional.empty());

        return last.map(type::cast).orElse(null);

    }

    protected <T> List<T> listCached(Class<T> type, Key key, Callable<List<T>> c) {
        return listCached(type, key, c, true);
    }

    protected <T> List<T> listCached(Class<T> type, Key key, Callable<List<T>> c, boolean cacheLast) {

        if (type == null || key == null || c == null) {
            return null;
        }

        Pair lastKey = Pair.of(type, Key.build(key).timestamp(null).build());

        Cache<Key, Optional<List<?>>> cache = listCache.computeIfAbsent(type, this::createCache);

        synchronized (cache) {

            int retry = 0;

            try {

                while (true) {

                    try {

                        Optional<List<?>> cached = cache.get(key, () -> {

                            List<T> values = c.call();

                            log.trace("Cached list : {} ({})", key, values == null ? null : values.size());

                            return Optional.ofNullable(values).map(Collections::unmodifiableList);

                        });

                        if (cacheLast) {
                            listLast.put(lastKey, cached);
                        }

                        @SuppressWarnings("unchecked")
                        List<T> result = (List<T>) cached.orElse(null);

                        return result;

                    } catch (Exception e) {

                        if (CACHE_RETRY < ++retry) {

                            log.warn("Failed to cache list : {} - {}", type, e);

                            if (cacheLast) {
                                cache.put(key, listLast.getOrDefault(lastKey, Optional.empty()));
                            } else {
                                cache.put(key, Optional.empty());
                            }

                            break;

                        }

                        Thread.sleep(CACHE_SLEEP.toMillis());

                    }

                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt(); // Propagate interruption.

            }

        }

        Optional<List<?>> last = listLast.getOrDefault(lastKey, Optional.empty());

        @SuppressWarnings("unchecked")
        List<T> result = (List<T>) last.orElse(null);

        return result;

    }

    private <K0, K1 extends K0, V0, V1 extends V0> Cache<K1, V1> createCache(Class<?> type) {

        log.trace("Creating cache : {}", type);

        return CacheBuilder.newBuilder()
                .maximumSize(CACHE_SIZE)
                .expireAfterWrite(CACHE_DURATION.toMillis(), MILLISECONDS)
                .build();

    }

    protected BigDecimal round(BigDecimal value, RoundingMode mode, BigDecimal unit) {

        if (value == null || mode == null || unit == null || unit.signum() == 0) {
            return null;
        }

        BigDecimal units = value.divide(unit, INTEGER_ZERO, mode);

        return units.multiply(unit);

    }

    protected Duration getTimeout() {

        long timeout = getLongProperty("timeout", FUTURE_TIMEOUT.toMillis());

        return Duration.ofMillis(timeout);

    }

    protected <V> V extract(Future<V> future) throws Exception {
        return extract(future, getTimeout());
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

    protected <V> V extractQuietly(Future<V> future) {
        try {
            return extract(future);
        } catch (Exception e) {
            return null;
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
    public StateType getState(Key key) {
        return state.get();
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
    public BigDecimal getBestAskSize(Key key) {
        return null;
    }

    @Override
    public BigDecimal getBestBidSize(Key key) {
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
    public Map<BigDecimal, BigDecimal> getAskPrices(Key key) {

        BigDecimal p = getBestAskPrice(key);

        BigDecimal s = getBestAskSize(key);

        return singletonMap(p, s);

    }

    @Override
    public Map<BigDecimal, BigDecimal> getBidPrices(Key key) {

        BigDecimal p = getBestBidPrice(key);

        BigDecimal s = getBestBidSize(key);

        return singletonMap(p, s);

    }

    @Override
    public List<Trade> listTrades(Key key, Instant fromTime) {
        return null;
    }

    @Override
    public CurrencyType getInstrumentCurrency(Key key) {
        return null;
    }

    @Override
    public CurrencyType getFundingCurrency(Key key) {
        return null;
    }

    @Override
    public String findProduct(Key key, CurrencyType instrument, CurrencyType funding) {
        return null;
    }

    @Override
    public BigDecimal getConversionPrice(Key key, CurrencyType currency) {
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

    protected Map<CreateInstruction, String> handleCreates(Set<CreateInstruction> instructions,
                                                           Converter<CreateInstruction, Future<String>> execute,
                                                           Converter<String, String> extract) {

        Map<CreateInstruction, Future<String>> futures = new IdentityHashMap<>();

        for (CreateInstruction i : trimToEmpty(instructions)) {

            if (i == null || i.getPrice() == null || i.getPrice().signum() == 0
                    || i.getSize() == null || i.getSize().signum() == 0) {

                futures.put(i, CompletableFuture.completedFuture(null));

                continue;

            }

            try {

                futures.put(i, execute.convert(i));

            } catch (Exception e) {

                futures.put(i, Futures.immediateFailedFuture(e));

            }

        }

        Instant cutoff = getNow().plus(getTimeout());

        Map<CreateInstruction, String> results = new IdentityHashMap<>();

        for (Entry<CreateInstruction, Future<String>> entry : futures.entrySet()) {

            try {

                Duration remaining = Duration.between(getNow(), cutoff);

                long millis = Math.max(remaining.toMillis(), FUTURE_MINIMUM.toMillis());

                String data = entry.getValue().get(millis, MILLISECONDS);

                results.put(entry.getKey(), extract.convert(data));

            } catch (TimeoutException e) {

                log.warn("Order create timeout : " + entry.getKey());

                results.put(entry.getKey(), null);

            } catch (Exception e) {

                log.warn("Order create failure : " + entry.getKey(), e);

                results.put(entry.getKey(), null);

            }

        }

        return results;

    }

    @Override
    public Map<CancelInstruction, String> cancelOrders(Key key, Set<CancelInstruction> instructions) {
        return null;
    }

    protected Map<CancelInstruction, String> handleCancels(Set<CancelInstruction> instructions,
                                                           Converter<CancelInstruction, Future<String>> execute,
                                                           Converter<String, String> extract) {

        Map<CancelInstruction, Future<String>> futures = new IdentityHashMap<>();

        for (CancelInstruction i : trimToEmpty(instructions)) {

            if (i == null || StringUtils.isEmpty(i.getId())) {

                futures.put(i, CompletableFuture.completedFuture(null));

                continue;

            }

            try {

                futures.put(i, execute.convert(i));

            } catch (Exception e) {

                futures.put(i, Futures.immediateFailedCheckedFuture(e));

            }

        }

        Instant cutoff = getNow().plus(getTimeout());

        Map<CancelInstruction, String> results = new IdentityHashMap<>();

        for (Entry<CancelInstruction, Future<String>> entry : futures.entrySet()) {

            try {

                Duration remaining = Duration.between(getNow(), cutoff);

                long millis = Math.max(remaining.toMillis(), FUTURE_MINIMUM.toMillis());

                String data = entry.getValue().get(millis, MILLISECONDS);

                results.put(entry.getKey(), extract.convert(data));

            } catch (TimeoutException e) {

                log.warn("Order cancel timeout : " + entry.getKey());

                results.put(entry.getKey(), null);

            } catch (Exception e) {

                log.warn("Order cancel failure : " + entry.getKey(), e);

                results.put(entry.getKey(), null);

            }

        }

        return results;

    }

}
