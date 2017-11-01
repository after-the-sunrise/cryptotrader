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
@ToString(exclude = {"buyOrderId", "sellOrderId"})
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BitmexTrade implements Trade {

    @SerializedName("timestamp")
    private Instant timestamp;

    @SerializedName("price")
    private BigDecimal price;

    @SerializedName("size")
    private BigDecimal size;

    private String buyOrderId;

    private String sellOrderId;

}
