package com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck;

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
public class CoincheckTick {

    @SerializedName("last")
    private BigDecimal last;

    @SerializedName("ask")
    private BigDecimal ask;

    @SerializedName("bid")
    private BigDecimal bid;

}
