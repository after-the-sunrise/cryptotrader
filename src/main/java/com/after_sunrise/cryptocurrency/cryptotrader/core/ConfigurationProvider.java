package com.after_sunrise.cryptocurrency.cryptotrader.core;

import com.google.inject.Provider;
import org.apache.commons.configuration2.Configuration;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface ConfigurationProvider extends Provider<Configuration> {

    void clear();

}
