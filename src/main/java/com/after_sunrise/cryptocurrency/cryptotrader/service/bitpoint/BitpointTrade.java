package com.after_sunrise.cryptocurrency.cryptotrader.service.bitpoint;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
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
public class BitpointTrade implements Trade {

    @SerializedName("id")
    private String id;

    @SerializedName("time")
    private Instant timestamp;

    @SerializedName("price")
    private BigDecimal price;

    @SerializedName("qty")
    private BigDecimal size;

}
