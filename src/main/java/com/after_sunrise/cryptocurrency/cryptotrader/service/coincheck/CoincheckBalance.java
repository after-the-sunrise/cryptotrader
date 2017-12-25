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
public class CoincheckBalance {

    @SerializedName("success")
    private Boolean success;

    @SerializedName("jpy")
    private BigDecimal jpy;

    @SerializedName("btc")
    private BigDecimal btc;

}
