package com.after_sunrise.cryptocurrency.cryptotrader;

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

        Cryptotrader trader = Guice.createInjector(module).getInstance(Cryptotrader.class);

        try {

            trader.execute();

        } finally {

            trader.shutdown();

        }

        log.info("Stopped application.");

    }

    @Override
    protected void configure() {

        bind(Cryptotrader.class).toInstance(proxy(Cryptotrader.class));

    }

    private <P> P proxy(Class<P> clazz) {

        ClassLoader cl = getClass().getClassLoader();

        Class[] classes = {clazz};

        Object o = Proxy.newProxyInstance(cl, classes, (p, m, a) -> null);

        return clazz.cast(o);

    }

}
