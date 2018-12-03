package com.after_sunrise.cryptocurrency.cryptotrader.service.bitpoint;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BitpointOrder implements Order {

    // 0: All, 1: Pending, 2: Active, 3: Filled, 5: Amending, 6: Amended
    // 7: Cancelling, 8: Cancelled, 9: Expired, 10: Stop
    public static final Set<String> ORDER_TERMINATED = Sets.newHashSet("8", "9");

    // 0: None, 1: Complete, 2: Partial
    public static final Set<String> EXECUTION_FILLED = Sets.newHashSet("1");

    public static final String SIDE_BUY = "3";

    @SerializedName("orderNo")
    private String id;

    @SerializedName("orderStatus")
    private String orderStatus;

    @SerializedName("executionStatus")
    private String executionStatus;

    @SerializedName("orderDt")
    private String date;

    @SerializedName("currencyCd1")
    private String currency1;

    @SerializedName("currencyCd2")
    private String currency2;

    @SerializedName("buySellCls")
    private String side;

    @SerializedName("orderPrice")
    private BigDecimal orderPrice;

    @SerializedName("orderNominal")
    private BigDecimal orderSize;

    @SerializedName("execNominal")
    private BigDecimal fillSize;

    @Override
    public String getProduct() {
        return currency1 + currency2;
    }

    @Override
    public Boolean getActive() {
        return !ORDER_TERMINATED.contains(orderStatus) && !EXECUTION_FILLED.contains(executionStatus);
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
        return orderSize != null && fillSize != null ? convertQuantity(orderSize.subtract(fillSize)) : null;
    }

    @VisibleForTesting
    BigDecimal convertQuantity(BigDecimal value) {
        return value == null ? null : SIDE_BUY.equals(side) ? value : value.negate();
    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Container {

        public static final Integer SUCCESS = 0;

        @SerializedName("resultCode")
        private Integer result;

        @SerializedName("vcOrderList")
        private List<BitpointOrder> orders;

    }

}
