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
     * Amount in wallet.
     */
    @SerializedName("walletBalance")
    private BigDecimal walletBalance;

    /**
     * Margin = Wallet + Unrealized
     */
    @SerializedName("marginBalance")
    private BigDecimal marginBalance;

    /**
     * Excess = Balance - Used
     */
    @SerializedName("excessMargin")
    private BigDecimal excess;

}
