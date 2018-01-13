package com.after_sunrise.cryptocurrency.cryptotrader.service.zaif;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface ZaifService extends Service {

    String ID = "zaif";

    @Override
    default String get() {
        return ID;
    }

}
