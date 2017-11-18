package com.after_sunrise.cryptocurrency.cryptotrader.service.bitfinex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.LastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MidEstimator;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface BitfinexService extends Service {

    String ID = "bitfinex";

    @Override
    default String get() {
        return ID;
    }

    class BitfinexLastEstimator extends LastEstimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return convertKey(context, request, ID);
        }
    }

    class BitfinexMidEstimator extends MidEstimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return convertKey(context, request, ID);
        }
    }

}
