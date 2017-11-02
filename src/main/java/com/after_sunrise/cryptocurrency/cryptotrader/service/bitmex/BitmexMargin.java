package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

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
public class BitmexMargin {

    /**
     * "XBt"
     */
    @SerializedName("currency")
    private String currency;

    /**
     * Margin = Wallet + Unrealized
     */
    @SerializedName("marginBalance")
    private BigDecimal balance;

}
