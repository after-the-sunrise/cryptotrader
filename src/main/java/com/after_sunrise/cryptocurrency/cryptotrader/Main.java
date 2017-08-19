package com.after_sunrise.cryptocurrency.cryptotrader;

import com.after_sunrise.cryptocurrency.cryptotrader.core.CryptotraderImpl;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Proxy;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class Main extends AbstractModule {

    public static final String MODULE = "cryptotrader.module";

    public static void main(String... args) throws Exception {

        String moduleName = System.getProperty(MODULE, Main.class.getName());

        Module module = (Module) Class.forName(moduleName).newInstance();

        log.info("Starting application : {}", moduleName);

        CryptotraderImpl trader = new CryptotraderImpl(Guice.createInjector(module));

        try {

            trader.execute();

        } finally {

            trader.shutdown();

        }

        log.info("Stopped application.");

    }

    @Override
    protected void configure() {

        bind(Trader.class).toInstance(proxy(Trader.class));

    }

    private <P> P proxy(Class<P> clazz) {

        ClassLoader cl = getClass().getClassLoader();

        Class[] classes = {clazz};

        Object o = Proxy.newProxyInstance(cl, classes, (p, m, a) -> null);

        return clazz.cast(o);

    }

}
