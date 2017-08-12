package com.after_sunrise.cryptocurrency.cryptotrader.core;

import java.util.concurrent.ExecutorService;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface ExecutorFactory extends AutoCloseable {

    ExecutorService get(Class<?> clazz);

}
