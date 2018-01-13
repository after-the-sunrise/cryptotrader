package com.after_sunrise.cryptocurrency.cryptotrader.service.bitfinex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;

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

}
