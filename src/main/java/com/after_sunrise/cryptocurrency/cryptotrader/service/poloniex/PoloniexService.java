package com.after_sunrise.cryptocurrency.cryptotrader.service.poloniex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.LastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MicroEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MidEstimator;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface PoloniexService extends Service {

    String ID = "poloniex";

    @Override
    default String get() {
        return ID;
    }

    class PoloniexLastEstimator extends LastEstimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return convertKey(context, request, ID);
        }
    }

    class PoloniexMicroEstimator extends MicroEstimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return convertKey(context, request, ID);
        }
    }

    class PoloniexMidEstimator extends MidEstimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return convertKey(context, request, ID);
        }
    }

}
