package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexService.SideType;
import com.google.gson.annotations.SerializedName;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexService.SideType.BUY;

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
    private String oid;

    /**
     * Id optionally assigned by the user.
     */
    @SerializedName("clOrdID")
    private String cid;

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
    private BigDecimal quantity;

    @Override
    public String getOrderId() {
        return StringUtils.isNotEmpty(cid) ? cid : oid;
    }

    @Override
    public BigDecimal getSize() {
        return quantity == null || BUY == SideType.find(side) ? quantity : quantity.negate();
    }

}
