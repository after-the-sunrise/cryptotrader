package com.after_sunrise.cryptocurrency.cryptotrader.service.fisco;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FiscoBalance {

    @SerializedName("funds")
    private Map<String, BigDecimal> funds;

    @SerializedName("deposit")
    private Map<String, BigDecimal> deposits;

    @SerializedName("rights")
    private Map<String, BigDecimal> rights;

    @SerializedName("open_orders")
    private Long orders;

    @SerializedName("trade_count")
    private Long trades;

    @SerializedName("server_time")
    private Instant timestamp;

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Container {

        private static final Integer SUCCESS = 1;

        @SerializedName("success")
        private Integer status;

        @SerializedName("return")
        private FiscoBalance balance;

        public Boolean isSuccess() {
            return SUCCESS.equals(status) && balance != null;
        }

    }

}
