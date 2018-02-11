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
public class QuoinexAccount {

    @SerializedName("currency")
    private String currency;

    @SerializedName("balance")
    private BigDecimal balance;

}
