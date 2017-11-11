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
public class BitmexBook {

    public static final String SIDE_BUY = "Buy";

    /**
     * "Buy", "Sell"
     */
    @SerializedName("side")
    private String side;

    /**
     * Price
     */
    @SerializedName("price")
    private BigDecimal price;

    /**
     * Size
     */
    @SerializedName("size")
    private BigDecimal size;

}
