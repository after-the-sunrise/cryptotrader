package com.after_sunrise.cryptocurrency.cryptotrader.core;

import java.time.Instant;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface Environment {

    Instant getNow();

    String getVersion();

}
