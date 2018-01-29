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
public class BtcboxTick {

    @SerializedName("buy")
    private BigDecimal buy;

    @SerializedName("sell")
    private BigDecimal sell;

    @SerializedName("last")
    private BigDecimal last;

}
