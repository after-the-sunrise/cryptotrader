package com.after_sunrise.cryptocurrency.cryptotrader.service.quoinex;

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
public class QuoinexExecution implements Order.Execution {

    @SerializedName("id")
    private final String id;

    private final String orderId;

    @SerializedName("created_at")
    private final Instant time;

    @SerializedName("my_side")
    private final String side;

    @SerializedName("price")
    private final BigDecimal price;

    @SerializedName("quantity")
    private final BigDecimal quantity;

    @Override
    public BigDecimal getSize() {

        if (quantity != null) {

            if ("buy".equals(side)) {
                return quantity;
            }

            if ("sell".equals(side)) {
                return quantity.negate();
            }

        }

        return null;

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
        private final List<QuoinexExecution> values;

    }

}
