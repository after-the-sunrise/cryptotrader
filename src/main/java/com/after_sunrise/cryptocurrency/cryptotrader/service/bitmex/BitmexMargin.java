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

    @SerializedName("currency")
    private String currency;

    @SerializedName("walletBalance")
    private BigDecimal walletBalance;

    @SerializedName("marginBalance")
    private BigDecimal marginBalance;

}
