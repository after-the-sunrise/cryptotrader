package com.after_sunrise.cryptocurrency.cryptotrader.service.bitfinex;

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
public class BitfinexTrade implements Trade {

    @SerializedName("timestamp")
    private Instant timestamp;

    @SerializedName("price")
    private BigDecimal price;

    @SerializedName("amount")
    private BigDecimal size;

}
