package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.SATOSHI;
import static java.math.RoundingMode.HALF_UP;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@ToString(exclude = "lock")
public class BitflyerTrade implements Trade {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final AtomicLong count = new AtomicLong();

    private final Instant timestamp;

    private final AtomicReference<BigDecimal> notional = new AtomicReference<>();

    private final AtomicReference<BigDecimal> volume = new AtomicReference<>();

    public BitflyerTrade(Instant timestamp, BigDecimal price, BigDecimal size) {
        this.count.incrementAndGet();
        this.timestamp = timestamp;
        this.notional.set(price.multiply(size));
        this.volume.set(size);
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public BigDecimal getPrice() {

        BigDecimal n;

        BigDecimal s;

        try {

            lock.readLock().lock();

            n = notional.get();

            s = volume.get();

        } finally {
            lock.readLock().unlock();
        }

        return s.signum() == 0 ? null : n.divide(s, SATOSHI.scale(), HALF_UP);

    }

    @Override
    public BigDecimal getSize() {
        return volume.get();
    }

    @Override
    public String getBuyOrderId() {
        return null;
    }

    @Override
    public String getSellOrderId() {
        return null;
    }

    public void accumulate(BigDecimal price, BigDecimal size) {

        try {

            lock.writeLock().lock();

            count.incrementAndGet();

            notional.accumulateAndGet(price.multiply(size), BigDecimal::add);

            volume.accumulateAndGet(size, BigDecimal::add);

        } finally {
            lock.writeLock().unlock();
        }

    }

    public Trade snapshot() {

        BigDecimal p;

        BigDecimal s;

        try {

            lock.readLock().lock();

            p = getPrice();

            s = getSize();

        } finally {
            lock.readLock().unlock();
        }

        return new BitflyerTrade(getTimestamp(), p, s);

    }

}
