package com.after_sunrise.cryptocurrency.cryptotrader.service.quoinex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.google.common.annotations.VisibleForTesting;
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
public class QuoinexOrder implements Order {

    @SerializedName("id")
    private final String id;

    @SerializedName("currency_pair_code")
    private final String product;

    @SerializedName("status")
    private final String status;

    @SerializedName("side")
    private final String side;

    @SerializedName("price")
    private final BigDecimal orderPrice;

    @SerializedName("quantity")
    private final BigDecimal orderSize;

    @SerializedName("filled_quantity")
    private final BigDecimal fillSize;

    @Override
    public Boolean getActive() {
        return "live".equals(status);
    }

    @VisibleForTesting
    BigDecimal convertQuantity(BigDecimal value) {

        if (value != null) {

            if ("buy".equals(side)) {
                return value;
            }

            if ("sell".equals(side)) {
                return value.negate();
            }

        }

        return null;

    }

    @Override
    public BigDecimal getOrderQuantity() {
        return convertQuantity(orderSize);
    }

    @Override
    public BigDecimal getFilledQuantity() {
        return convertQuantity(fillSize);
    }

    @Override
    public BigDecimal getRemainingQuantity() {

        if (orderSize == null || fillSize == null) {
            return null;
        }

        return convertQuantity(orderSize.subtract(fillSize));

    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Container {

        @SerializedName("current_page")
        private final Long pageCurrent;

        @SerializedName("total_pages")
        private final Long pageTotal;

        @SerializedName("models")
        private final List<QuoinexOrder> values;

    }

}
