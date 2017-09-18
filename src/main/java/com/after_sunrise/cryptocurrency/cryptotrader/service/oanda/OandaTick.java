package com.after_sunrise.cryptocurrency.cryptotrader.service.oanda;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OandaTick {

    @SerializedName("instrument")
    private String instrument;

    @SerializedName("time")
    private Instant timestamp;

    @SerializedName("ask")
    private BigDecimal ask;

    @SerializedName("bid")
    private BigDecimal bid;

}
