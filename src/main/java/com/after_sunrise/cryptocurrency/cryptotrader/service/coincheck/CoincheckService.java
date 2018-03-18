package com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
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
public interface CoincheckService extends Service {

    String ID = "coincheck";

    @Override
    default String get() {
        return ID;
    }

    enum ProductType {

        BTC_JPY(BTC, JPY, new BigDecimal("0.005"), ONE);

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

        ProductType(CurrencyType instrument, CurrencyType funding, BigDecimal lotSize, BigDecimal tickSize) {
            this.id = name().toLowerCase();
            this.instrumentCurrency = instrument;
            this.fundingCurrency = funding;
            this.lotSize = lotSize;
            this.tickSize = tickSize;
        }

    }

    enum SideType {

        BUY(true),

        SELL(false),

        MARKET_BUY(true),

        MARKET_SELL(false),

        LEVERAGE_BUY(true),

        LEVERAGE_SELL(false),

        CLOSE_LONG(false),

        CLOSE_SHORT(true);

        private static final Map<String, SideType> IDS = Stream.of(values()).collect(
                Collectors.toMap(SideType::getId, Function.identity())
        );

        public static Optional<SideType> find(String id) {
            return Optional.ofNullable(IDS.get(id));
        }

        @Getter
        private final String id;

        @Getter
        private final boolean buy;

        SideType(boolean buy) {
            this.id = name().toLowerCase();
            this.buy = buy;
        }

    }

}
