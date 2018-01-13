package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.stream.Stream;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.*;
import static java.util.stream.Collectors.toMap;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface BitmexService extends Service {

    String ID = "bitmex";

    @Override
    default String get() {
        return ID;
    }

    enum SideType {

        BUY,

        SELL;

        private final String id = StringUtils.capitalize(name().toLowerCase());

        public String getId() {
            return id;
        }

        public static SideType find(String id) {
            return Stream.of(values()).filter(e -> e.getId().equals(id)).findAny().orElse(null);
        }

    }

    enum FundingType {

        XBT("XBt", BTC);

        @Getter
        private final String id;

        @Getter
        private final CurrencyType currency;

        private static final Map<String, FundingType> ID = Stream.of(values())
                .collect(toMap(FundingType::getId, e -> e));

        public static FundingType findById(String id) {
            return ID.get(id);
        }

        FundingType(String id, CurrencyType currency) {
            this.id = id;
            this.currency = currency;
        }

    }

    enum ProductType {

        /**
         * Cash
         */
        XBT("XBT", null, BTC, BTC, 1),

        /**
         * Minutely Bitcoin Price Index
         */
        BXBT(".BXBT", null, BTC, USD, null),

        /**
         * Half-Hour Bitcoin Price Index
         */
        BXBT30M(".BXBT30M", null, BTC, USD, null),

        /**
         * XBT/USD Swap
         */
        XBTUSD("XBTUSD", "XBT:perpetual", BTC, USD, 1),

        /**
         * XBT/USD Swap Funding Rate
         */
        XBT_FR(null, "XBT:perpetual", null, null, null),

        /**
         * XBT/USD Monthly Futures
         */
        XBT_MT(null, "XBT:monthly", BTC, USD, 1),

        /**
         * XBT/USD Quarterly Futures
         */
        XBT_QT(null, "XBT:quarterly", BTC, USD, 1),

        /**
         * XBT/JPY Monthly Futures
         */
        XBJ_MT(null, "XBJ:monthly", BTC, JPY, 100),

        /**
         * XBT/JPY Quarterly Futures
         */
        XBJ_QT(null, "XBJ:quarterly", BTC, JPY, 100),

        /**
         * XBT/JPY BitMEX Price Index
         */
        BXBTJPY(".BXBTJPY", null, BTC, JPY, null),

        /**
         * 30-Minute XBT/JPY BitMEX Price Index
         */
        BXBTJPY30M(".BXBTJPY30M", null, BTC, JPY, null),

        /**
         * Minutely Ether Price Index
         */
        ETHXBT(".ETHXBT", null, ETH, BTC, null),

        /**
         * 30-Minute Ether Price Index
         */
        ETHXBT30M(".ETHXBT30M", null, ETH, BTC, null),

        /**
         * ETH/XBT Futures
         */
        ETH_QT(null, "ETH:quarterly", ETH, BTC, 1),

        /**
         * Minutely Ether Classic Price Index
         */
        ETCXBT(".ETCXBT", null, ETC, BTC, null),

        /**
         * 30-Minute Ether Classic Price Index
         */
        ETCXBT30M(".ETCXBT30M", null, ETC, BTC, null),

        /**
         * ETC/XBT Classic Futures
         */
        ETC_WK(null, "ETC:weekly", ETC, BTC, 1),

        /**
         * Minutely BCH Price Index
         */
        BCHXBT(".BCHXBT", null, BCH, BTC, null),

        /**
         * 30-Minute BCH Price Index
         */
        BCHXBT30M(".BCHXBT30M", null, BCH, BTC, null),

        /**
         * BCH/XBT Futures
         */
        BCH_MT(null, "BCH:monthly", BCH, BTC, 1);

        private static final Map<String, ProductType> NAME = Stream.of(values())
                .collect(toMap(Enum::name, e -> e));

        public static ProductType findByName(String name) {
            return NAME.get(name);
        }

        @Getter
        private final String id;

        @Getter
        private final String alias;

        @Getter
        private final CurrencyType structure;

        @Getter
        private final CurrencyType funding;

        @Getter
        private final Integer multiplier;

        ProductType(String id, String alias, CurrencyType structure, CurrencyType funding, Integer multiplier) {
            this.id = id;
            this.alias = alias;
            this.structure = structure;
            this.funding = funding;
            this.multiplier = multiplier;
        }

    }

}
