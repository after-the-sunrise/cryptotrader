package com.after_sunrise.cryptocurrency.cryptotrader.service.poloniex;

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
public class PoloniexTick {

    @SerializedName("last")
    private BigDecimal last;

    @SerializedName("lowestAsk")
    private BigDecimal ask;

    @SerializedName("highestBid")
    private BigDecimal bid;

}
