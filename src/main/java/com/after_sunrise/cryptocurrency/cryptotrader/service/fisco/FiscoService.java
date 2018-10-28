package com.after_sunrise.cryptocurrency.cryptotrader.service.fisco;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface FiscoService extends Service {

    String ID = "fisco";

    @Override
    default String get() {
        return ID;
    }

    enum ProductType {

        BTC_JPY("btc_jpy", BTC, JPY, new BigDecimal("0.001"), new BigDecimal("5")),

        BCH_JPY("bch_jpy", BCH, JPY, new BigDecimal("0.001"), new BigDecimal("5")),

        BCH_BTC("bch_btc", BCH, BTC, new BigDecimal("0.0001"), new BigDecimal("0.0001"));

        private static final Map<String, ProductType> NAMES = Stream.of(values()).collect(
                Collectors.toMap(Enum::name, Function.identity())
        );

        public static ProductType find(String name) {
            return NAMES.get(name);
        }

        @Getter
        private final String id;

        @Getter
        private final CurrencyType instrumentCurrency;

        @Getter
        private final CurrencyType fundingCurrency;

        @Getter
        private final BigDecimal lotSize;

        @Getter
        private final BigDecimal tickSize;

        ProductType(String id, CurrencyType instrument, CurrencyType funding, BigDecimal lotSize, BigDecimal tickSize) {
            this.id = id;
            this.instrumentCurrency = instrument;
            this.fundingCurrency = funding;
            this.lotSize = lotSize;
            this.tickSize = tickSize;
        }

    }

}
