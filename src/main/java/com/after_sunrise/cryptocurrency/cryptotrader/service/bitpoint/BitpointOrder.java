package com.after_sunrise.cryptocurrency.cryptotrader.service.bitpoint;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BitpointOrder implements Order {

    @SerializedName("orderId")
    private String id;

    @SerializedName("symbol")
    private String product;

    @SerializedName("tradeType")
    private String tradeType;

    @SerializedName("isWorking")
    private Boolean active;

    @SerializedName("time")
    private Instant time;

    @SerializedName("side")
    private String side;

    @SerializedName("price")
    private BigDecimal orderPrice;

    @SerializedName("orderQty")
    private BigDecimal orderSize;

    @SerializedName("executedQty")
    private BigDecimal filledSize;

    BigDecimal convertQuantity(BigDecimal value) {
        return value == null ? null : "BUY".equals(side) ? value : value.negate();
    }

    @Override
    public BigDecimal getOrderQuantity() {
        return convertQuantity(orderSize);
    }

    @Override
    public BigDecimal getFilledQuantity() {
        return convertQuantity(filledSize);
    }

    @Override
    public BigDecimal getRemainingQuantity() {

        if (orderSize == null || filledSize == null) {
            return null;
        }

        return convertQuantity(orderSize.subtract(filledSize));

    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Container {

        @SerializedName("orders")
        private List<BitpointOrder> orders;

    }

}
