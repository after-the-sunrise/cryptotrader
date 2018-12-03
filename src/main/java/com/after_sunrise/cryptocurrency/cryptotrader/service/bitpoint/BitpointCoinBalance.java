package com.after_sunrise.cryptocurrency.cryptotrader.service.bitpoint;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BitpointCoinBalance {

    public static final Integer SUCCESS = 0;

    @SerializedName("resultCode")
    private Integer result;

    @SerializedName("vcBalanceList")
    private List<Balance> balances;

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Balance {

        @SerializedName("currencyCd1")
        private String currency1;

        @SerializedName("currencyCd2")
        private String currency2;

        @SerializedName("nominal")
        private BigDecimal amount;

    }

}
