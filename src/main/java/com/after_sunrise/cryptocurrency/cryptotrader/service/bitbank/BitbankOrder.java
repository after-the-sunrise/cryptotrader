package com.after_sunrise.cryptocurrency.cryptotrader.service.bitbank;

import cc.bitbank.entity.enums.OrderSide;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@ToString
public class BitbankOrder implements Order {

    private static final Set<String> ACTIVES = Sets.newHashSet(
            "UNFILLED",
            "PARTIALLY_FILLED"
    );

    private static final Set<String> CANCELS = Sets.newHashSet(
            "CANCELED_UNFILLED",
            "CANCELED_PARTIALLY_FILLED"
    );

    private final cc.bitbank.entity.Order delegate;

    public BitbankOrder(cc.bitbank.entity.Order delegate) {
        this.delegate = delegate;
    }

    public cc.bitbank.entity.Order getDelegate() {
        return delegate;
    }

    @Override
    public String getId() {
        return String.valueOf(delegate.orderId);
    }

    @Override
    public String getProduct() {
        return delegate.pair;
    }

    @Override
    public Boolean getActive() {
        return ACTIVES.contains(delegate.status);
    }

    public Boolean getCancelled() {
        return CANCELS.contains(delegate.status);
    }

    @Override
    public BigDecimal getOrderPrice() {
        return delegate.price;
    }

    @VisibleForTesting
    BigDecimal convertSideQuantity(BigDecimal quantity) {

        if (quantity == null || delegate.side == null) {
            return null;
        }

        return delegate.side == OrderSide.BUY ? quantity : quantity.negate();

    }

    @Override
    public BigDecimal getOrderQuantity() {
        return convertSideQuantity(delegate.startAmount);
    }

    @Override
    public BigDecimal getFilledQuantity() {
        return convertSideQuantity(delegate.executedAmount);
    }

    @Override
    public BigDecimal getRemainingQuantity() {
        return convertSideQuantity(delegate.remainingAmount);
    }

    @ToString
    public static class BitbankExecution implements Execution {

        private final cc.bitbank.entity.Order delegate;

        public BitbankExecution(cc.bitbank.entity.Order delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getId() {
            return String.valueOf(delegate.orderId);
        }

        @Override
        public String getOrderId() {
            return String.valueOf(delegate.orderId);
        }

        @Override
        public Instant getTime() {
            return delegate.executedAt == null ? null : Instant.ofEpochMilli(delegate.executedAt.getTime());
        }

        @Override
        public BigDecimal getPrice() {
            return delegate.averagePrice;
        }

        @Override
        public BigDecimal getSize() {
            return delegate.executedAmount;
        }

    }

}
