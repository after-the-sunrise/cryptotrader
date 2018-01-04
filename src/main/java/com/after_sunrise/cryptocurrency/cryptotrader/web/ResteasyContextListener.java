package com.after_sunrise.cryptocurrency.cryptotrader.web;

import com.after_sunrise.cryptocurrency.cryptotrader.Cryptotrader;
import com.after_sunrise.cryptocurrency.cryptotrader.core.ConfigurationProvider;
import com.after_sunrise.cryptocurrency.cryptotrader.core.CryptotraderImpl;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;

import javax.annotation.PreDestroy;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class ResteasyContextListener extends GuiceResteasyBootstrapServletContextListener {

    // [Lifecycle]
    // 1. Module#configure
    // 2. ContextListener#withInjector
    // 3. ContextListener#postConstruct
    // (running -> stop requested)
    // 4. ContextListener#preDestroy

    @Override
    protected void withInjector(Injector injector) {

        Cryptotrader trader = injector.getInstance(Cryptotrader.class);

        CountDownLatch latch = injector.getInstance(CountDownLatch.class);

        ExecutorService executor = Executors.newCachedThreadPool();

        try {

            executor.execute(trader::execute);

            executor.submit(() -> {

                log.debug("Awaiting until termination...");

                latch.await();

                log.debug("Shutting down trader.");

                trader.shutdown();

                return null;

            });

        } finally {

            executor.shutdown();

        }

    }

    @Slf4j
    public static class Module extends CryptotraderImpl.Module {

        private final CountDownLatch latch = new CountDownLatch(1);

        @Override
        protected void configure() {

            super.configure();

            bind(CountDownLatch.class).toInstance(latch);

            binder().bind(EndpointImpl.class).asEagerSingleton();

        }

        @PreDestroy
        public void preDestroy() {

            log.debug("Preparing for destruction.");

            latch.countDown();

        }

    }

    @Slf4j
    @Path("/rest")
    public static class EndpointImpl {

        private final Trader trader;

        private final ConfigurationProvider configurationProvider;

        @Inject
        public EndpointImpl(Injector injector) {

            this.trader = injector.getInstance(Trader.class);

            this.configurationProvider = injector.getInstance(ConfigurationProvider.class);

        }

        @POST
        @Path("/trade")
        public void triggerTrader() {

            log.debug("Triggering trader.");

            trader.trigger();

        }

        @POST
        @Path("/reload")
        public void reloadConfiguration() {

            log.debug("Reloading configuration.");

            configurationProvider.clear();

        }

    }

}
