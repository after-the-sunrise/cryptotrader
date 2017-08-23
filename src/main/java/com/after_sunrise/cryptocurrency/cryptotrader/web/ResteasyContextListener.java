package com.after_sunrise.cryptocurrency.cryptotrader.web;

import com.after_sunrise.cryptocurrency.cryptotrader.Cryptotrader;
import com.after_sunrise.cryptocurrency.cryptotrader.core.CryptotraderImpl;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;

import javax.annotation.PreDestroy;
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

        executor.execute(trader::execute);

        executor.submit(() -> {

            latch.await();

            trader.shutdown();

            return null;

        });

        executor.shutdown();

    }

    @Slf4j
    public static class Module extends CryptotraderImpl.Module {

        private final CountDownLatch latch = new CountDownLatch(1);

        @Override
        protected void configure() {

            super.configure();

            bind(CountDownLatch.class).toInstance(latch);

        }

        @PreDestroy
        public void preDestroy() {

            latch.countDown();

        }

    }

}
