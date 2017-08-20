package com.after_sunrise.cryptocurrency.cryptotrader.web;

import com.after_sunrise.cryptocurrency.cryptotrader.Cryptotrader;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class ResteasyContextListener extends GuiceResteasyBootstrapServletContextListener {

    @Override
    protected void withInjector(Injector injector) {

        Cryptotrader trader = injector.getInstance(Cryptotrader.class);

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(trader::execute);

        executor.execute(trader::shutdown);

        executor.shutdown();

    }

}
