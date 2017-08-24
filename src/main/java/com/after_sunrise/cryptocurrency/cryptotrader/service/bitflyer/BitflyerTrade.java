package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.entity.Execution;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@ToString
public class BitflyerTrade implements Trade {

    private final Execution delegate;

    public BitflyerTrade(Execution delegate) {
        this.delegate = delegate;
    }

    @Override
    public Instant getTimestamp() {
        return delegate.getTimestamp() == null ? null : delegate.getTimestamp().toInstant();
    }

    @Override
    public BigDecimal getPrice() {
        return delegate.getPrice();
    }

    @Override
    public BigDecimal getSize() {
        return delegate.getSize();
    }

    @Override
    public String getBuyOrderId() {
        return delegate.getBuyOrderId();
    }

    @Override
    public String getSellOrderId() {
        return delegate.getSellOrderId();
    }

}
