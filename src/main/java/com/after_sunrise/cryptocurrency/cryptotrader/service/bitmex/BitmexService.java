package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;
import com.after_sunrise.cryptocurrency.cryptotrader.service.composite.CompositeService.CompositeLastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.composite.CompositeService.CompositeMidEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.LastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MidEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.VwapEstimator;
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

        BXBT(".BXBT", null, BTC, USD, null),

        BXBT30M(".BXBT30M", null, BTC, USD, null),

        XBTUSD("XBTUSD", "XBT:perpetual", BTC, USD, 1),

        XBT_QT(null, "XBT:quarterly", BTC, USD, 1),

        XBJ_QT(null, "XBJ:quarterly", BTC, JPY, 100),

        BXBTJPY(".BXBTJPY", null, BTC, JPY, null),

        BXBTJPY30M(".BXBTJPY30M", null, BTC, JPY, null),

        ETHXBT(".ETHXBT", null, ETH, BTC, null),

        ETHXBT30M(".ETHXBT30M", null, ETH, BTC, null),

        ETH_QT(null, "ETH:quarterly", ETH, BTC, 1);

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

    class BitmexLastEstimator extends LastEstimator {
        @Override
        public Context.Key getKey(Request request) {
            return convertKey(request, ID);
        }
    }

    class BitmexMidEstimator extends MidEstimator {
        @Override
        public Context.Key getKey(Request request) {
            return convertKey(request, ID);
        }
    }

    class BitmexVwapEstimator extends VwapEstimator {
        @Override
        public Context.Key getKey(Request request) {
            return convertKey(request, ID);
        }
    }

    class BitmexCompositeMidEstimator extends CompositeMidEstimator {
        @Override
        public Context.Key getKey(Request request) {
            return convertKey(request, ID);
        }
    }

    class BitmexCompositeLastEstimator extends CompositeLastEstimator {
        @Override
        public Context.Key getKey(Request request) {
            return convertKey(request, ID);
        }
    }

}
