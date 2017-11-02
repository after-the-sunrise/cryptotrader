package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

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
public class BitmexPosition {

    /**
     * "XTBUSD", "XBTZ17", "XBJZ17", ".BXBT"
     */
    @SerializedName("symbol")
    private String symbol;

    /**
     * Number of contracts.
     */
    @SerializedName("currentQty")
    private BigDecimal quantity;

}
