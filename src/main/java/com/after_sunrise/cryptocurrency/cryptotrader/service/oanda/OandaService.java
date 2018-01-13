package com.after_sunrise.cryptocurrency.cryptotrader.service.oanda;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;

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

}
