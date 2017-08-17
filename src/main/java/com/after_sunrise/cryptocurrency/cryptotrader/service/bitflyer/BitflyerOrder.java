package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType;
import com.after_sunrise.cryptocurrency.bitflyer4j.core.StateType;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.OrderList.Response;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@ToString
public class BitflyerOrder implements Order {

    private final Response delegate;

    public BitflyerOrder(Response delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getId() {
        return delegate.getAcceptanceId();
    }

    @Override
    public String getProduct() {
        return delegate.getProduct();
    }

    @Override
    public Boolean getActive() {
        return delegate.getState() == StateType.ACTIVE;
    }

    @Override
    public BigDecimal getOrderPrice() {
        return delegate.getPrice();
    }

    @Override
    public BigDecimal getOrderQuantity() {

        BigDecimal filled = getFilledQuantity();

        if (filled == null) {
            return null;
        }

        BigDecimal remaining = getRemainingQuantity();

        if (remaining == null) {
            return null;
        }

        return filled.add(remaining);

    }

    @Override
    public BigDecimal getFilledQuantity() {

        BigDecimal value = delegate.getExecutedSize();

        if (value == null) {
            return null;
        }

        return delegate.getSide() == SideType.BUY ? value : value.negate();

    }

    @Override
    public BigDecimal getRemainingQuantity() {

        BigDecimal value = delegate.getOutstandingSize();

        if (value == null) {
            return null;
        }

        return delegate.getSide() == SideType.BUY ? value : value.negate();

    }

}
