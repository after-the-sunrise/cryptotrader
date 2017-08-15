package com.after_sunrise.cryptocurrency.cryptotrader.core;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface ServiceFactory {

    <T> List<T> load(Class<T> clazz);

    <K, T extends Supplier<K>> Map<K, T> loadMap(Class<T> clazz);

}
