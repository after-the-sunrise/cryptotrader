package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.AssetType.*;
import static java.math.BigDecimal.ONE;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface BitflyerService {

    String ID = "bitflyer";

    BigDecimal SATOSHI = new BigDecimal("0.00000001");

    enum AssetType {

        JPY(ONE),

        BTC(SATOSHI),

        BCH(SATOSHI),

        ETH(SATOSHI),

        ETC(SATOSHI),

        LTC(SATOSHI),

        COLLATERAL(ONE),

        FX_BTC(new BigDecimal("0.001")),

        FUTURE_BTC1W(new BigDecimal("0.001")),

        FUTURE_BTC2W(new BigDecimal("0.001"));

        private static Map<String, AssetType> NAMES = stream(values()).collect(toMap(AssetType::name, t -> t));

        public static AssetType find(String name) {
            return NAMES.get(name);
        }

        @Getter
        private final BigDecimal unit;

        @Getter
        private final String code;

        AssetType(BigDecimal unit) {
            this(unit, null);
        }

        AssetType(BigDecimal unit, String code) {
            this.unit = unit;
            this.code = code == null ? name() : code;
        }

        public BigDecimal roundToUnit(BigDecimal value, RoundingMode mode) {

            if (value == null || mode == null) {
                return null;
            }

            BigDecimal units = value.divide(unit, INTEGER_ZERO, mode);

            return units.multiply(unit);

        }

    }

    enum ProductType {

        BTC_JPY(BTC, JPY, new BigDecimal("0.001"), ONE),

        BCH_BTC(BCH, BTC, new BigDecimal("0.01"), new BigDecimal("0.00001")),

        ETH_BTC(ETH, BTC, new BigDecimal("0.01"), new BigDecimal("0.00001")),

        FX_BTC_JPY(FX_BTC, COLLATERAL, new BigDecimal("0.001"), ONE),

        BTCJPY_MAT1WK(FUTURE_BTC1W, COLLATERAL, new BigDecimal("0.001"), ONE),

        BTCJPY_MAT2WK(FUTURE_BTC2W, COLLATERAL, new BigDecimal("0.001"), ONE);

        private static Map<String, ProductType> NAMES = stream(values()).collect(toMap(ProductType::name, t -> t));

        public static ProductType find(String name) {
            return NAMES.get(name);
        }

        @Getter
        private final AssetType structure;

        @Getter
        private final AssetType funding;

        @Getter
        private final BigDecimal lotSize;

        @Getter
        private final BigDecimal tickSize;

        ProductType(AssetType structure, AssetType funding, BigDecimal lotSize, BigDecimal tickSize) {
            this.structure = structure;
            this.funding = funding;
            this.lotSize = lotSize;
            this.tickSize = tickSize;
        }

        public BigDecimal roundToLotSize(BigDecimal value, RoundingMode mode) {

            if (value == null || mode == null) {
                return null;
            }

            BigDecimal units = value.divide(lotSize, INTEGER_ZERO, mode);

            return units.multiply(lotSize);

        }

        public BigDecimal roundToTickSize(BigDecimal value, RoundingMode mode) {

            if (value == null || mode == null) {
                return null;
            }

            BigDecimal units = value.divide(tickSize, INTEGER_ZERO, mode);

            return units.multiply(tickSize);

        }


    }

}
