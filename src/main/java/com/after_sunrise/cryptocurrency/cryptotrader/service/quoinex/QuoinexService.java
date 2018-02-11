package com.after_sunrise.cryptocurrency.cryptotrader.service.quoinex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.BTC;
import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.JPY;
import static java.math.BigDecimal.ONE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface QuoinexService extends Service {

    String ID = "quoinex";

    @Override
    default String get() {
        return ID;
    }

    enum ProductType {

        BTC_JPY("BTCJPY", BTC, JPY, new BigDecimal("0.001"), ONE);

        private static final Map<String, ProductType> NAMES = Stream.of(values()).collect(
                Collectors.toMap(Enum::name, Function.identity())
        );

        public static ProductType find(String name) {
            return NAMES.get(name);
        }

        @Getter
        private final String code;

        @Getter
        private final CurrencyType instrumentCurrency;

        @Getter
        private final CurrencyType fundingCurrency;

        @Getter
        private final BigDecimal lotSize;

        @Getter
        private final BigDecimal tickSize;

        ProductType(String code, CurrencyType instrument, CurrencyType funding,
                    BigDecimal lotSize, BigDecimal tickSize) {
            this.code = code;
            this.instrumentCurrency = instrument;
            this.fundingCurrency = funding;
            this.lotSize = lotSize;
            this.tickSize = tickSize;
        }

    }

}
