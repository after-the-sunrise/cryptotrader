package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.Bitflyer4j;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.Board;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.Board.Quote;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.RealtimeListener.RealtimeListenerAdapter;
import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.MarketEstimator.Context;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class BitflyerMarketContext extends RealtimeListenerAdapter implements Context {

    private static final Comparator<BigDecimal> COMPARATOR_DECIMAL = Comparator //
            .nullsLast((o1, o2) -> Comparator.<BigDecimal>naturalOrder().compare(o1, o2));

    private static final Comparator<Quote> COMPARATOR_QUOTE = Comparator //
            .nullsLast((o1, o2) -> COMPARATOR_DECIMAL.compare(o1.getPrice(), o2.getPrice()));

    private static final ZoneId ZONE = ZoneId.of("GMT");

    private static final int SIZE = 24 * 60;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<String, NavigableMap<Instant, Board>> boards = new HashMap<>();

    private PropertyManager propertyManager;

    private Bitflyer4j bitflyer4j;

    @Inject
    public void initialize(Injector injector) {

        propertyManager = injector.getInstance(PropertyManager.class);

        bitflyer4j = injector.getInstance(Bitflyer4j.class);

        bitflyer4j.getRealtimeService().addListener(this);

    }

    @Override
    public void onBoards(String product, Board value) {

        try {

            lock.writeLock().lock();

            NavigableMap<Instant, Board> map = boards.computeIfAbsent(product, p -> new TreeMap<>());

            Instant now = propertyManager.getNow();

            Instant trimmed = now.atZone(ZONE).truncatedTo(ChronoUnit.MINUTES).toInstant();

            map.put(trimmed, value);

            while (map.size() > SIZE) {

                Instant key = map.firstKey();

                map.remove(key);

            }

        } finally {
            lock.writeLock().unlock();
        }

    }

    @Override
    public String get() {
        return BitflyerService.ID;
    }

    @VisibleForTesting
    <V, R> R forInstant(Map<String, NavigableMap<Instant, V>> map, String instrument, Instant timestamp, Function<V, R> function) {

        V value;

        try {

            lock.readLock().lock();

            NavigableMap<Instant, V> times = map.get(instrument);

            if (MapUtils.isEmpty(times)) {
                return null;
            }

            Entry<Instant, V> entry;

            if (timestamp == null) {
                entry = times.floorEntry(propertyManager.getNow());
            } else {
                entry = times.floorEntry(timestamp);
            }

            if (entry == null) {
                return null;
            }

            value = entry.getValue();

        } finally {
            lock.readLock().unlock();
        }

        return function.apply(value);

    }

    @VisibleForTesting
    SortedSet<Quote> getSortedQuotes(Collection<Quote> quotes) {

        if (CollectionUtils.isEmpty(quotes)) {
            return Collections.emptySortedSet();
        }

        SortedSet<Quote> sorted = new TreeSet<>(COMPARATOR_QUOTE);

        quotes.stream().filter(q -> q.getPrice() != null && q.getSize() != null).forEach(sorted::add);

        return sorted;

    }

    @Override
    public BigDecimal getBesAskPrice(String site, String instrument, Instant timestamp) {

        bitflyer4j.getRealtimeService().subscribeBoard(Arrays.asList(instrument));

        return forInstant(boards, instrument, timestamp, board -> {

            SortedSet<Quote> quotes = getSortedQuotes(board.getAsk());

            if (CollectionUtils.isEmpty(quotes)) {
                return null;
            }

            return quotes.first().getPrice();

        });

    }

    @Override
    public BigDecimal getBesBidPrice(String site, String instrument, Instant timestamp) {

        bitflyer4j.getRealtimeService().subscribeBoard(Arrays.asList(instrument));

        return forInstant(boards, instrument, timestamp, board -> {

            SortedSet<Quote> quotes = getSortedQuotes(board.getBid());

            if (CollectionUtils.isEmpty(quotes)) {
                return null;
            }

            return quotes.last().getPrice();

        });

    }

}
