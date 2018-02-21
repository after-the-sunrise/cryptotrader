package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.impl.AbstractService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
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

        public HttpUriRequest create(String url, Map<String, String> headers, String data) {

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

    private static final Duration CACHE_DURATION = Duration.ofMinutes(1);

    private static final Duration CACHE_SLEEP = Duration.of(100, MILLIS);

    private static final int CACHE_RETRY = 2;

    private final Map<Class<?>, Cache<Key, Optional<?>>> singleCache = synchronizedMap(new HashMap<>());

    private final Map<Class<?>, Cache<Key, Optional<List<?>>>> listCache = synchronizedMap(new HashMap<>());

    private final String id;

    private final CloseableHttpClient client;

    private final AtomicReference<StateType> state;

    protected TemplateContext(String id) {

        this.id = id;

        this.client = HttpClients.createDefault();

        this.state = new AtomicReference<>(StateType.ACTIVE);

    }

    @Override
    public String get() {
        return id;
    }

    @Override
    public void close() throws Exception {

        client.close();

        state.set(StateType.TERMINATE);

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

        StringBuilder sb = new StringBuilder();

        if (MapUtils.isNotEmpty(parameters)) {

            for (Map.Entry<String, String> entry : parameters.entrySet()) {

                if (StringUtils.isEmpty(entry.getKey())) {
                    continue;
                }

                if (StringUtils.isEmpty(entry.getValue())) {
                    continue;
                }

                sb.append(sb.length() == 0 ? "?" : "&");
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

        HttpUriRequest request = type.create(path, headers, data);

        return client.execute(request, response -> {

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            response.getEntity().writeTo(out);

            String body = new String(out.toByteArray(), UTF_8);

            StatusLine statusLine = response.getStatusLine();

            LOG.trace("[RECV][{}][{}] {}", statusLine, response.getAllHeaders(), body);

            if (HttpStatus.SC_OK == statusLine.getStatusCode()) {
                return body;
            }

            throw new IOException(statusLine + " : " + body);

        });

    }

    @VisibleForTesting
    public void clear() {

        log.trace("Clearing cache.");

        singleCache.forEach((k, v) -> v.invalidateAll());

        listCache.forEach((k, v) -> v.invalidateAll());

    }

    protected <T> T findCached(Class<T> type, Key key, Callable<T> c) {

        if (type == null || key == null || c == null) {
            return null;
        }

        Cache<Key, Optional<?>> cache = singleCache.computeIfAbsent(type, t -> createCache());

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

                        return cached.map(type::cast).orElse(null);

                    } catch (Exception e) {

                        if (CACHE_RETRY < ++retry) {

                            log.warn("Failed to cache : {} - {}", type, e);

                            break;

                        }

                        Thread.sleep(CACHE_SLEEP.toMillis());

                    }

                }

            } catch (InterruptedException e) {
                // Do nothing.
            }

        }

        return null;

    }

    protected <T> List<T> listCached(Class<T> type, Key key, Callable<List<T>> c) {

        if (type == null || key == null || c == null) {
            return null;
        }

        Cache<Key, Optional<List<?>>> cache = listCache.computeIfAbsent(type, t -> createCache());

        synchronized (cache) {

            int retry = 0;

            try {

                while (true) {

                    try {

                        Optional<List<?>> cached = cache.get(key, () -> {

                            List<T> values = c.call();

                            log.trace("Cached list : {} ({})", key, values == null ? null : values.size());

                            return Optional.ofNullable(values);

                        });

                        return cached.map(l -> l.stream().map(type::cast).collect(toList()))
                                .map(Collections::unmodifiableList).orElse(null);

                    } catch (Exception e) {

                        if (CACHE_RETRY < ++retry) {

                            log.warn("Failed to cache list : {} - {}", type, e);

                            break;

                        }

                        Thread.sleep(CACHE_SLEEP.toMillis());

                    }

                }

            } catch (InterruptedException e) {
                // Do nothing.
            }

        }

        return null;

    }

    private <K0, K1 extends K0, V0, V1 extends V0> Cache<K1, V1> createCache() {
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

    @Override
    public Map<CancelInstruction, String> cancelOrders(Key key, Set<CancelInstruction> instructions) {
        return null;
    }

}
