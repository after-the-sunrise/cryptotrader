package com.after_sunrise.cryptocurrency.cryptotrader.service.quoinex;

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
public class QuoinexProduct {

    @SerializedName("id")
    private String id;

    @SerializedName("currency_pair_code")
    private String code;

    @SerializedName("last_traded_price")
    private BigDecimal lastPrice;

    @SerializedName("last_traded_quantity")
    private BigDecimal lastSize;

    @SerializedName("taker_fee")
    private BigDecimal takerFee;

    @SerializedName("maker_fee")
    private BigDecimal makerFee;

}
