package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.Bitflyer4j;
import com.after_sunrise.cryptocurrency.bitflyer4j.core.ProductType;
import com.after_sunrise.cryptocurrency.bitflyer4j.core.StateType;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.*;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.RealtimeListener.RealtimeListenerAdapter;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.RealtimeService;
import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ZONE;
import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.time.ZonedDateTime.ofInstant;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import static org.apache.commons.lang3.math.NumberUtils.LONG_ONE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class BitflyerContext extends RealtimeListenerAdapter implements Context {

    private static final ChronoUnit CACHE_UNIT = MINUTES;

    private static final int CACHE_SIZE = 60;

    private final Map<String, NavigableMap<Instant, Board>> boards = new ConcurrentHashMap<>();

    private final Map<String, NavigableMap<Instant, Tick>> ticks = new ConcurrentHashMap<>();

    private final Map<String, NavigableMap<Instant, List<Execution>>> execs = new ConcurrentHashMap<>();

    private final Cache<Instant, List<Balance>> balances = newBuilder().expireAfterWrite(5L, SECONDS).build();

    private final Cache<Instant, List<OrderList.Response>> orders = newBuilder().expireAfterWrite(5L, SECONDS).build();

    private PropertyManager propertyManager;

    private Bitflyer4j bitflyer4j;

    @Inject
    public void initialize(Injector injector) {

        propertyManager = injector.getInstance(PropertyManager.class);

        bitflyer4j = injector.getInstance(Bitflyer4j.class);

        bitflyer4j.getRealtimeService().addListener(this);

        log.debug("Initialized.");

    }

    @Override
    public void onBoards(String product, Board value) {

        if (StringUtils.isEmpty(product) || value == null) {
            return;
        }

        NavigableMap<Instant, Board> map = boards.computeIfAbsent(product, p -> new ConcurrentSkipListMap<>());

        ZonedDateTime time = ofInstant(propertyManager.getNow(), ZONE);

        Instant trimmed = time.plus(LONG_ONE, CACHE_UNIT).truncatedTo(CACHE_UNIT).toInstant();

        map.put(trimmed, value);

        log.trace("Stored Board : {} - {} - {}", product, trimmed, value);

        while (map.size() > Math.max(CACHE_SIZE, INTEGER_ZERO)) {

            Instant key = map.firstKey();

            map.remove(key);

            log.trace("Cleaned Board : {} - {}", product, key);

        }

    }

    @Override
    public void onTicks(String product, List<Tick> values) {

        if (StringUtils.isEmpty(product) || CollectionUtils.isEmpty(values)) {
            return;
        }

        NavigableMap<Instant, Tick> map = ticks.computeIfAbsent(product, p -> new ConcurrentSkipListMap<>());

        values.stream().filter(v -> v.getTimestamp() != null).forEach(value -> {

            ZonedDateTime time = value.getTimestamp();

            Instant trimmed = time.plus(LONG_ONE, CACHE_UNIT).truncatedTo(CACHE_UNIT).toInstant();

            map.put(trimmed, value);

            log.trace("Stored Tick : {} - {} - {}", product, trimmed, value);

            while (map.size() > Math.max(CACHE_SIZE, INTEGER_ZERO)) {

                Instant key = map.firstKey();

                map.remove(key);

                log.trace("Cleaned Tick : {} - {}", product, key);

            }

        });

    }

    @Override
    public void onExecutions(String product, List<Execution> values) {

        if (StringUtils.isEmpty(product) || CollectionUtils.isEmpty(values)) {
            return;
        }

        NavigableMap<Instant, List<Execution>> map = execs.computeIfAbsent(product, p -> new ConcurrentSkipListMap<>());

        values.stream().filter(v -> v.getTimestamp() != null).forEach(value -> {

            ZonedDateTime time = value.getTimestamp();

            Instant trimmed = time.plus(LONG_ONE, CACHE_UNIT).truncatedTo(CACHE_UNIT).toInstant();

            List<Execution> list = map.computeIfAbsent(trimmed, t -> new CopyOnWriteArrayList<>());

            list.add(value);

            log.trace("Stored Execution : {} - {} - {}", product, trimmed, value);

            while (map.size() > Math.max(CACHE_SIZE, INTEGER_ZERO)) {

                Instant key = map.firstKey();

                List<Execution> cleaned = map.remove(key);

                log.trace("Cleaned Execution : {} - {} ({})", product, key, cleaned.size());

            }

        });

    }


    @Override
    public String get() {
        return BitflyerService.ID;
    }

    @VisibleForTesting
    <V, R> R forInstant(Map<String, NavigableMap<Instant, V>> instruments, Key key, //
                        BiConsumer<RealtimeService, String> subscriber, Function<V, R> function) {

        if (key == null) {
            return null;
        }

        String instrument = trimToEmpty(key.getInstrument());

        NavigableMap<Instant, V> times = instruments.get(instrument);

        if (MapUtils.isEmpty(times)) {

            subscriber.accept(bitflyer4j.getRealtimeService(), instrument);

            return null;

        }

        Instant time = key.getTimestamp() != null ? key.getTimestamp() : propertyManager.getNow();

        Entry<Instant, V> entry = times.floorEntry(time);

        if (entry == null) {
            return null;
        }

        return function.apply(entry.getValue());

    }

    @Override
    public BigDecimal getBesAskPrice(Key key) {
        return forInstant(ticks, key, (s, i) -> {

            log.trace("Subscribing ask : {}", i);

            s.subscribeTick(singletonList(i));

        }, Tick::getBestAskPrice);
    }

    @Override
    public BigDecimal getBesBidPrice(Key key) {
        return forInstant(ticks, key, (s, i) -> {

            log.trace("Subscribing bid : {}", i);

            s.subscribeTick(singletonList(i));

        }, Tick::getBestBidPrice);
    }

    @Override
    public BigDecimal getMidPrice(Key key) {
        return forInstant(boards, key, (s, i) -> {

            log.trace("Subscribing mid : {}", i);

            s.subscribeBoard(singletonList(i));

        }, Board::getMid);
    }

    @Override
    public BigDecimal getLastPrice(Key key) {
        return forInstant(ticks, key, (s, i) -> {

            log.trace("Subscribing last : {}", i);

            s.subscribeTick(singletonList(i));

        }, Tick::getTradePrice);
    }

    @VisibleForTesting
    BigDecimal forBalance(Key key, Function<ProductType, ProductType> mapper, Function<Balance, BigDecimal> function) {

        if (key == null) {
            return null;
        }

        Instant time = ofInstant(propertyManager.getNow(), ZONE).truncatedTo(ChronoUnit.SECONDS).toInstant();

        List<Balance> cached;

        try {

            cached = balances.get(time, () -> {

                List<Balance> values = bitflyer4j.getAccountService().getBalances().get();

                log.trace("Queried balances : {}", values);

                return values;

            });

        } catch (ExecutionException e) {

            log.trace("Failed to query balances : " + key, e);

            return null;

        }

        ProductType instrumentType = ProductType.find(key.getInstrument());

        if (instrumentType == null) {
            return null;
        }

        String currency = mapper.apply(instrumentType).name();

        for (Balance balance : cached) {
            if (StringUtils.equals(currency, balance.getCurrency())) {
                return balance.getAmount();
            }
        }

        return null;

    }

    @Override
    public BigDecimal getInstrumentPosition(Key key) {

        // TODO : Handle margin products (FX + Futures)

        return forBalance(key, ProductType::getStructure, Balance::getAvailable);
    }

    @Override
    public BigDecimal getFundingPosition(Key key) {

        // TODO : Handle margin products (FX + Futures)

        return forBalance(key, ProductType::getFunding, Balance::getAvailable);

    }

    @Override
    public BigDecimal roundLotSize(Key key, BigDecimal value, RoundingMode mode) {

        if (key == null || value == null || mode == null) {
            return null;
        }

        ProductType type = ProductType.find(key.getInstrument());

        if (type == null) {
            return null;
        }

        return type.roundToLotSize(value, mode);

    }

    @Override
    public BigDecimal roundTickSize(Key key, BigDecimal value, RoundingMode mode) {

        if (key == null || value == null || mode == null) {
            return null;
        }

        ProductType instrument = ProductType.find(key.getInstrument());

        if (instrument == null) {
            return null;
        }

        return instrument.roundToTickSize(value, mode);

    }

    @Override
    public List<OrderList.Response> getOrders(Key key, StateType state) {

        if (key == null) {
            return null;
        }

        Instant time = ofInstant(propertyManager.getNow(), ZONE).truncatedTo(ChronoUnit.SECONDS).toInstant();

        try {

            List<OrderList.Response> cached = orders.get(time, () -> {

                OrderList ol = OrderList.builder().state(state).product(key.getInstrument()).build();

                List<OrderList.Response> vals = bitflyer4j.getOrderService().listOrders(ol, null).get();

                log.trace("Queried orders : {}", vals);

                return unmodifiableList(vals);

            });

            return cached;

        } catch (ExecutionException e) {

            log.trace("Failed to query orders : " + key, e);

            return Collections.emptyList();

        }

    }

}
