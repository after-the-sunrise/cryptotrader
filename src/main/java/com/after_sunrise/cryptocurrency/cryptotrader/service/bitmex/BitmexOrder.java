package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.math.BigDecimal;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BitmexOrder implements Order {

    static final String SIDE_BUY = "Buy";
    static final String SIDE_SELL = "Sell";

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
     * Order activation
     */
    @SerializedName("workingIndicator")
    private Boolean active;

    /**
     * "XTBUSD", "XBTZ17", "XBJZ17"
     */
    @SerializedName("symbol")
    private String product;

    /**
     * "Buy"
     */
    @SerializedName("side")
    private String side;

    /**
     * Limit price.
     */
    @SerializedName("price")
    private BigDecimal orderPrice;

    /**
     * Order quantity.
     */
    @SerializedName("orderQty")
    private BigDecimal orderQuantity;

    /**
     * Filled quantity.
     */
    @SerializedName("cumQty")
    private BigDecimal filledQuantity;

    /**
     * Remaining quantity.
     */
    @SerializedName("leavesQty")
    private BigDecimal remainingQuantity;

    @Override
    public String getId() {
        return clientId != null ? clientId : orderId;
    }

}
