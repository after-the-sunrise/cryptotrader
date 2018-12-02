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
public class BitpointExecution implements Order.Execution {

    @SerializedName("symbol")
    private String symbol;

    @SerializedName("id")
    private String id;

    @SerializedName("orderId")
    private String orderId;

    @SerializedName("tradeId")
    private String tradeId;

    @SerializedName("price")
    private BigDecimal price;

    @SerializedName("qty")
    private BigDecimal size; // TODO: Negate for Sell

    @SerializedName("commission")
    private BigDecimal commission;

    @SerializedName("commissionAsset")
    private String commissionAsset;

    @SerializedName("time")
    private Instant time;

    @SerializedName("tradeType")
    private String tradeType;

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Container {

        @SerializedName("trades")
        private List<BitpointExecution> trades;

    }

}
