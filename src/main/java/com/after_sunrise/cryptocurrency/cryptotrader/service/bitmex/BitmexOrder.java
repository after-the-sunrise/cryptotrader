package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexService.SideType;
import com.google.gson.annotations.SerializedName;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexService.SideType.BUY;

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
    private BigDecimal quantity;

    /**
     * Filled quantity.
     */
    @SerializedName("cumQty")
    private BigDecimal filled;

    /**
     * Remaining quantity.
     */
    @SerializedName("leavesQty")
    private BigDecimal remaining;

    @Override
    public String getId() {
        return StringUtils.isNotEmpty(clientId) ? clientId : orderId;
    }

    @Override
    public BigDecimal getOrderQuantity() {
        return quantity == null || BUY == SideType.find(side) ? quantity : quantity.negate();
    }

    @Override
    public BigDecimal getFilledQuantity() {
        return filled == null || BUY == SideType.find(side) ? filled : filled.negate();
    }

    @Override
    public BigDecimal getRemainingQuantity() {
        return remaining == null || BUY == SideType.find(side) ? remaining : remaining.negate();
    }

}
