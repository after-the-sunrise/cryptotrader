package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType;
import com.after_sunrise.cryptocurrency.bitflyer4j.core.StateType;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.OrderList;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.ParentList;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface BitflyerOrder extends Order {

    @ToString
    class Child implements BitflyerOrder {

        private final OrderList delegate;

        public Child(OrderList delegate) {
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
        public BigDecimal getFilledQuantity() {
            return convertSize(delegate.getSide(), delegate.getExecutedSize());
        }

        @Override
        public BigDecimal getRemainingQuantity() {
            return convertSize(delegate.getSide(), delegate.getOutstandingSize());
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

    }

    @ToString
    class Parent implements BitflyerOrder {

        private final ParentList delegate;

        public Parent(ParentList delegate) {
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
        public BigDecimal getFilledQuantity() {
            return convertSize(delegate.getSide(), delegate.getExecutedSize());
        }

        @Override
        public BigDecimal getRemainingQuantity() {
            return convertSize(delegate.getSide(), delegate.getOutstandingSize());
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

    }

    interface Visitor<T> {

        T visit(Child order);

        T visit(Parent order);

    }

    <T> T accept(Visitor<T> visitor);

    @Override
    default BigDecimal getOrderQuantity() {

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

    default BigDecimal convertSize(SideType side, BigDecimal size) {

        if (side == null || size == null) {
            return null;
        }

        return side == SideType.BUY ? size : size.negate();

    }

}
