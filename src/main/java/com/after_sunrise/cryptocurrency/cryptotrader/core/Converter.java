package com.after_sunrise.cryptocurrency.cryptotrader.core;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@FunctionalInterface
public interface Converter<I, O> {

    O convert(I input) throws Exception;

}
