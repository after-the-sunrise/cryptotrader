package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import java.util.function.Supplier;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface Service extends Supplier<String> {

    String WILDCARD = "*";

    int SCALE = 10;

}
