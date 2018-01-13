package com.after_sunrise.cryptocurrency.cryptotrader.service.poloniex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;

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

}
