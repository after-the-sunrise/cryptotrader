package com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck;

import com.google.gson.annotations.SerializedName;
import lombok.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CoincheckPagination {

    @SerializedName("limit")
    private Long limit;

    @SerializedName("starting_after")
    private Long start;

    @SerializedName("ending_before")
    private Long end;

    @SerializedName("order")
    private String order;

}
