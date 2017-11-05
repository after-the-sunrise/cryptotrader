package com.after_sunrise.cryptocurrency.cryptotrader.service.poloniex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;
import com.after_sunrise.cryptocurrency.cryptotrader.service.composite.CompositeService.CompositeLastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.composite.CompositeService.CompositeMidEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.LastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MidEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.VwapEstimator;

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
        public Context.Key getKey(Request request) {
            return convertKey(request, ID);
        }
    }

    class PoloniexMidEstimator extends MidEstimator {
        @Override
        public Context.Key getKey(Request request) {
            return convertKey(request, ID);
        }
    }

    class PoloniexVwapEstimator extends VwapEstimator {
        @Override
        public Context.Key getKey(Request request) {
            return convertKey(request, ID);
        }
    }

    class PoloniexCompositeMidEstimator extends CompositeMidEstimator {
        @Override
        public Context.Key getKey(Request request) {
            return convertKey(request, ID);
        }
    }

    class PoloniexCompositeLastEstimator extends CompositeLastEstimator {
        @Override
        public Context.Key getKey(Request request) {
            return convertKey(request, ID);
        }
    }

}
