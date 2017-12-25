package com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.LastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MicroEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MidEstimator;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        final String id;

        @Getter
        final boolean buy;

        SideType(boolean buy) {
            this.id = name().toLowerCase();
            this.buy = buy;
        }

    }

    class CoincheckLastEstimator extends LastEstimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return convertKey(context, request, ID);
        }
    }

    class CoincheckMicroEstimator extends MicroEstimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return convertKey(context, request, ID);
        }
    }

    class CoincheckMidEstimator extends MidEstimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return convertKey(context, request, ID);
        }
    }

}
