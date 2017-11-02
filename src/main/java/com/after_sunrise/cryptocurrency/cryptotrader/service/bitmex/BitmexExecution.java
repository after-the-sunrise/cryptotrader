package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BitmexExecution implements Order.Execution {

    /**
     * Id of the execution.
     */
    @SerializedName("execID")
    private String id;

    /**
     * Id assigned by the exchange.
     */
    @SerializedName("orderID")
    private String orderId;

    /**
     * Id optionally assigned by the user.
     */
    @SerializedName("clOrdID")
    private String clientId;

    /**
     * Execution price.
     */
    @SerializedName("transactTime")
    private Instant time;

    /**
     * "Buy", "Sell"
     */
    @SerializedName("side")
    private String side;

    /**
     * Execution price.
     */
    @SerializedName("lastPx")
    private BigDecimal price;

    /**
     * Execution quantity.
     */
    @SerializedName("lastQty")
    private BigDecimal size;

}
