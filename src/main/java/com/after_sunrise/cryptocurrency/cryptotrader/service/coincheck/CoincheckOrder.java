package com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck.CoincheckService.SideType;
import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CoincheckOrder implements Order {

    @SerializedName("pair")
    private String product;

    @SerializedName("id")
    private String id;

    @SerializedName("order_type")
    private String side;

    @SerializedName("rate")
    private BigDecimal orderPrice;

    @SerializedName("pending_amount")
    private BigDecimal quantity;

    @Override
    public Boolean getActive() {
        return Boolean.TRUE;
    }

    @Override
    public BigDecimal getOrderQuantity() {

        if (quantity == null) {
            return null;
        }

        return SideType.find(side)
                .map(s -> s.isBuy() ? quantity : quantity.negate())
                .orElse(null);

    }

    @Override
    public BigDecimal getFilledQuantity() {
        return null;
    }

    @Override
    public BigDecimal getRemainingQuantity() {
        return getOrderQuantity();
    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Container {

        @SerializedName("success")
        private Boolean success;

        @SerializedName("orders")
        private List<CoincheckOrder> orders;

    }

}
