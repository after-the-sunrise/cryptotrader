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

    /**
     * "XTBUSD", "XBTZ17", "XBJZ17", ".BXBT"
     */
    @SerializedName("symbol")
    private String symbol;

    /**
     * "XBt"
     */
    @SerializedName("settlCurrency")
    private String settleCurrency;

    /**
     * "Open", "Unlisted"
     */
    @SerializedName("state")
    private String state;

    /**
     * "yyyy-MM-ddTHH:mm:ss.SSSZ"
     */
    @SerializedName("timestamp")
    private Instant timestamp;

    /**
     * Last traded price.
     */
    @SerializedName("lastPrice")
    private BigDecimal last;

    /**
     * Best ask price.
     */
    @SerializedName("askPrice")
    private BigDecimal ask;

    /**
     * Best bid price.
     */
    @SerializedName("bidPrice")
    private BigDecimal bid;

    /**
     * Mid price.
     */
    @SerializedName("midPrice")
    private BigDecimal mid;

    /**
     * Quantity lot size.
     */
    @SerializedName("lotSize")
    private BigDecimal lotSize;

    /**
     * Price tick size.
     */
    @SerializedName("tickSize")
    private BigDecimal tickSize;

    /**
     * Contract expiry time.
     */
    @SerializedName("expiry")
    private Instant expiry;

    /**
     * ".BXBT", ".BXBTJPY", ".BXBT30M" ".BXBTJPY30M"
     */
    @SerializedName("referenceSymbol")
    private String reference;

    /**
     * Passive fee in real number. (1 bps = "0.0001")
     */
    @SerializedName("makerFee")
    private BigDecimal makerFee;

    /**
     * Aggressive fee in real number. (1 bps = "0.0001")
     */
    @SerializedName("takerFee")
    private BigDecimal takerFee;

    /**
     * Expiry fee in real number. (1 bps = "0.0001")
     */
    @SerializedName("settlementFee")
    private BigDecimal settleFee;

}
