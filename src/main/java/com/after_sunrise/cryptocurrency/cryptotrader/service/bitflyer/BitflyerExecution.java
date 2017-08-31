package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.TradeExecution;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Execution;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;

import static com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType.BUY;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@ToString
public class BitflyerExecution implements Execution {

    private final TradeExecution.Response delegate;

    public BitflyerExecution(TradeExecution.Response delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getId() {

        Long id = delegate.getId();

        return id == null ? null : id.toString();

    }

    @Override
    public String getOrderId() {
        return delegate.getAcceptanceId();
    }

    @Override
    public Instant getTime() {

        ZonedDateTime time = delegate.getExecDate();

        return time == null ? null : time.toInstant();

    }

    @Override
    public BigDecimal getPrice() {
        return delegate.getPrice();
    }

    @Override
    public BigDecimal getSize() {

        SideType side = delegate.getSide();

        BigDecimal value = delegate.getSize();

        if (side == null || value == null) {
            return null;
        }

        return side == BUY ? value : value.negate();

    }

}
