package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.util.List;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BitmexAlias {

    /**
     * "XBT:perpetual", "XBT:quarterly", "XBJ:quarterly"
     */
    @SerializedName("intervals")
    private List<String> intervals;

    /**
     * "XBTUSD", "XBTZ17", "XBJZ17"
     */
    @SerializedName("symbols")
    private List<String> symbols;

    public String find(String alias) {

        if (intervals == null) {
            return null;
        }

        int index = intervals.indexOf(alias);

        if (index < 0 || symbols.size() <= index) {
            return null;
        }

        return symbols.get(index);

    }

}
