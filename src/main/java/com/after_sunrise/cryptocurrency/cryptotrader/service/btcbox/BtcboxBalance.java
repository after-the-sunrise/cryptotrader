package com.after_sunrise.cryptocurrency.cryptotrader.service.btcbox;

import com.google.common.annotations.VisibleForTesting;
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
public class BtcboxBalance {

    @SerializedName("jpy_balance")
    private BigDecimal jpyBalance;

    @SerializedName("jpy_lock")
    private BigDecimal jpyLock;

    @SerializedName("btc_balance")
    private BigDecimal btcBalance;

    @SerializedName("btc_lock")
    private BigDecimal btcLock;

    public BigDecimal getJpy() {
        return sum(jpyBalance, jpyLock);
    }

    public BigDecimal getBtc() {
        return sum(btcBalance, btcLock);
    }

    @VisibleForTesting
    BigDecimal sum(BigDecimal v1, BigDecimal v2) {
        return v1 == null ? v2 : v2 == null ? v1 : v1.add(v2);
    }

}
