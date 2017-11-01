package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

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
public class BitmexTick {

    @SerializedName("symbol")
    private String symbol;

    @SerializedName("lastPrice")
    private BigDecimal last;

    @SerializedName("askPrice")
    private BigDecimal ask;

    @SerializedName("bidPrice")
    private BigDecimal bid;

    @SerializedName("midPrice")
    private BigDecimal mid;

    @SerializedName("markPrice")
    private BigDecimal mark;

    @SerializedName("lotSize")
    private BigDecimal lotSize;

    @SerializedName("tickSize")
    private BigDecimal tickSize;

    @SerializedName("expiry")
    private Instant expiry;

    @SerializedName("referenceSymbol")
    private String underlying;

    @SerializedName("makerFee")
    private BigDecimal makerFee;

    @SerializedName("takerFee")
    private BigDecimal takerFee;

    @SerializedName("settlementFee")
    private BigDecimal clearFee;

}
