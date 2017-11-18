package com.after_sunrise.cryptocurrency.cryptotrader.service.oanda;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.LastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MidEstimator;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface OandaService extends Service {

    String ID = "oanda";

    @Override
    default String get() {
        return ID;
    }

    class OandaLastEstimator extends LastEstimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return convertKey(context, request, ID);
        }
    }

    class OandaMidEstimator extends MidEstimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return convertKey(context, request, ID);
        }
    }

}
