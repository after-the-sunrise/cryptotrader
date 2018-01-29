package com.after_sunrise.cryptocurrency.cryptotrader.service.btcbox;

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
public class BtcboxResponse {

    @SerializedName("result")
    private boolean result;

    @SerializedName("id")
    private String id;

}
