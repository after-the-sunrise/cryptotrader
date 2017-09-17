package com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.Estimators;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.LastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MidEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.VwapEstimator;

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

    class CoincheckLastEstimator extends LastEstimator {
        @Override
        public Context.Key getKey(Request request) {
            return Estimators.getKey(request, ID);
        }
    }

    class CoincheckMidEstimator extends MidEstimator {
        @Override
        public Context.Key getKey(Request request) {
            return Estimators.getKey(request, ID);
        }
    }

    class CoincheckVwapEstimator extends VwapEstimator {
        @Override
        public Context.Key getKey(Request request) {
            return Estimators.getKey(request, ID);
        }
    }

}
