package com.after_sunrise.cryptocurrency.cryptotrader.service.quoinex;

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
public class QuoinexTrade implements Trade {

    @SerializedName("id")
    private String id;

    @SerializedName("created_at")
    private Instant timestamp;

    @SerializedName("price")
    private BigDecimal price;

    @SerializedName("quantity")
    private BigDecimal size;

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Container {

        @SerializedName("current_page")
        private final Long pageCurrent;

        @SerializedName("total_pages")
        private final Long pageTotal;

        @SerializedName("models")
        private final List<QuoinexTrade> trades;

    }

}
