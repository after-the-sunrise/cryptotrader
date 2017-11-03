package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
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
public class BitmexTrade implements Trade {

    /**
     * Id of this trade.
     */
    @SerializedName("trdMatchID")
    private String id;

    /**
     * Time of the trade.
     */
    @SerializedName("timestamp")
    private Instant timestamp;

    /**
     * Price of the trade.
     */
    @SerializedName("price")
    private BigDecimal price;

    /**
     * Number of contracts traded.
     */
    @SerializedName("size")
    private BigDecimal size;

    @Override
    public String getBuyOrderId() {
        return id;
    }

    @Override
    public String getSellOrderId() {
        return id;
    }

}
