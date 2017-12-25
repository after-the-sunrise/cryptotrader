package com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CoincheckTrade implements Trade {

    @SerializedName("id")
    private Long id;

    @SerializedName("created_at")
    private Instant timestamp;

    @SerializedName("rate")
    private BigDecimal price;

    @SerializedName("amount")
    private BigDecimal size;

    @Override
    public String getBuyOrderId() {
        return null;
    }

    @Override
    public String getSellOrderId() {
        return null;
    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Container {

        @SerializedName("success")
        private Boolean success;

        @SerializedName("pagination")
        private CoincheckPagination pagination;

        @SerializedName("data")
        private List<CoincheckTrade> trades;

    }

}
