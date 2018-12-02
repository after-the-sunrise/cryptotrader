package com.after_sunrise.cryptocurrency.cryptotrader.service.bitpoint;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BitpointBalance {

    @SerializedName("makerCommission")
    private BigDecimal makerCommission;

    @SerializedName("takerCommission")
    private BigDecimal takerCommission;

    @SerializedName("balances")
    private List<CoinBalance> coinBalances;

    @SerializedName("cashBalances")
    private List<CashBalance> cashBalances;

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CoinBalance {

        @SerializedName("asset")
        private String asset;

        @SerializedName("free")
        private BigDecimal free;

        @SerializedName("locked")
        private BigDecimal locked;

    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CashBalance {


        @SerializedName("fx")
        private List<Asset> fxAssets;

        @SerializedName("spot")
        private List<Asset> spotAssets;

        @SerializedName("leverage")
        private List<Asset> leverageAssets;

    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Asset {

        @SerializedName("asset")
        private String asset;

        @SerializedName("free")
        private BigDecimal free;

    }

}
