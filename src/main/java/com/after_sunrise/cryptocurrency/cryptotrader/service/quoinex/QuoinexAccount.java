package com.after_sunrise.cryptocurrency.cryptotrader.service.quoinex;

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
public class QuoinexAccount {

    @SerializedName("product_id")
    private String productId;

    @SerializedName("position")
    private BigDecimal position;

    @SerializedName("free_margin")
    private BigDecimal margin;

}
