package com.after_sunrise.cryptocurrency.cryptotrader.service.btcbox;

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
public class BtcboxBalance {

    @SerializedName("jpy_balance")
    private BigDecimal jpy;

    @SerializedName("btc_balance")
    private BigDecimal btc;

}
