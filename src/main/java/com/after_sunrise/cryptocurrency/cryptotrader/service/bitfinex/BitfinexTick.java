package com.after_sunrise.cryptocurrency.cryptotrader.service.bitfinex;

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
public class BitfinexTick {

    @SerializedName("last_price")
    private BigDecimal last;

    @SerializedName("ask")
    private BigDecimal ask;

    @SerializedName("bid")
    private BigDecimal bid;

}
