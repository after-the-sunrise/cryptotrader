package com.after_sunrise.cryptocurrency.cryptotrader;

import com.after_sunrise.cryptocurrency.cryptotrader.core.CryptotraderImpl;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class Web extends GuiceResteasyBootstrapServletContextListener {

    private Cryptotrader trader;

    @Override
    protected void withInjector(Injector injector) {
        trader = new CryptotraderImpl(injector);
    }

    @PostConstruct
    public void execute() {

        log.info("Executing trader.");

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> trader.execute());

        executor.shutdown();

    }

    @PreDestroy
    public void shutdown() {

        log.info("Terminating trader.");

        trader.shutdown();

    }

}
