package com.after_sunrise.cryptocurrency.cryptotrader.core;

import com.after_sunrise.cryptocurrency.bitflyer4j.Bitflyer4j;
import com.after_sunrise.cryptocurrency.bitflyer4j.Bitflyer4jFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.Cryptotrader;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.impl.*;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;

import java.util.concurrent.atomic.AtomicReference;


/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class CryptotraderImpl extends AbstractModule implements Cryptotrader {

    private final AtomicReference<Injector> injector = new AtomicReference<>();

    @Override
    protected void configure() {

        bind(Configuration.class).toInstance(new ConfigurationSupplier().get());
        bind(PropertyManager.class).to(PropertyManagerImpl.class).asEagerSingleton();

        bind(Bitflyer4j.class).toInstance(new Bitflyer4jFactory().createInstance());
        bind(ExecutorFactory.class).to(ExecutorFactoryImpl.class).asEagerSingleton();

        bind(Context.class).toProvider(ContextProvider.class).asEagerSingleton();
        bind(MarketEstimator.class).to(MarketEstimatorImpl.class).asEagerSingleton();
        bind(PortfolioAdviser.class).to(PortfolioAdviserImpl.class).asEagerSingleton();
        bind(OrderInstructor.class).to(OrderInstructorImpl.class).asEagerSingleton();

        bind(OrderManager.class).to(OrderManagerImpl.class).asEagerSingleton();
        bind(PipelineProcessor.class).to(PipelineProcessorImpl.class).asEagerSingleton();
        bind(Trader.class).to(TraderImpl.class).asEagerSingleton();

    }

    @Override
    public void execute() throws Exception {

        Injector i;

        synchronized (injector) {

            if (injector.get() != null) {

                log.warn("Skipping execution.");

                return;

            }

            i = Guice.createInjector(this);

            injector.set(i);

        }

        Trader trader = i.getInstance(Trader.class);

        log.info("Executing : {}", trader);

        trader.trade();

        log.info("Executed.");

    }

    @Override
    public void shutdown() {

        Injector i;

        synchronized (injector) {
            i = injector.getAndSet(null);
        }

        if (i == null) {

            log.warn("Skipping shutdown.");

            return;

        }

        try {
            i.getInstance(ExecutorFactory.class).close();
        } catch (Exception e) {
            log.warn("Failed to close ExecutorFactory.", e);
        }

        try {
            i.getInstance(Bitflyer4j.class).close();
        } catch (Exception e) {
            log.warn("Failed to close Bitflyer4j.", e);
        }

        log.info("Shutdown.");

    }

}
