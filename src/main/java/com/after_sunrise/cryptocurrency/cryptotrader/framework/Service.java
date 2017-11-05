package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import java.math.BigDecimal;
import java.util.function.Supplier;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface Service extends Supplier<String> {

    BigDecimal SATOSHI = new BigDecimal("0.00000001");

    enum CurrencyType {

        /**
         * Japanese Yen
         */
        JPY,

        /**
         * US Dollar
         */
        USD,

        /**
         * Bitcoin
         */
        BTC,

        /**
         * Bitcoin Cash
         */
        BCH,

        /**
         * Bitcoin Gold
         */
        BCG,

        /**
         * Ethereum
         */
        ETH,

        /**
         * Ethereum Classic
         */
        ETC,

        /**
         * Litecoin
         */
        LTC,

        /**
         * Monacoin
         */
        MONA

    }

}
