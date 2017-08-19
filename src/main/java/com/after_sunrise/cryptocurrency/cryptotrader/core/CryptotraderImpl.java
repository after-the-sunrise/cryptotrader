package com.after_sunrise.cryptocurrency.cryptotrader.core;

import com.after_sunrise.cryptocurrency.bitflyer4j.Bitflyer4j;
import com.after_sunrise.cryptocurrency.bitflyer4j.Bitflyer4jFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.Cryptotrader;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.impl.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;


/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class CryptotraderImpl implements Cryptotrader {

    @Slf4j
    public static class Module extends AbstractModule {

        private final Class<? extends Trader> traderClass;

        public Module() {
            this(TraderImpl.class);
        }

        public Module(Class<? extends Trader> traderClass) {
            this.traderClass = traderClass;
        }

        @Override
        protected void configure() {

            bind(Configuration.class).toInstance(new ConfigurationSupplier().get());
            bind(PropertyManager.class).to(PropertyManagerImpl.class).asEagerSingleton();
            bind(Environment.class).to(PropertyManager.class).asEagerSingleton();
            bind(ServiceFactory.class).to(ServiceFactoryImpl.class).asEagerSingleton();

            bind(Bitflyer4j.class).toInstance(new Bitflyer4jFactory().createInstance());
            bind(ExecutorFactory.class).to(ExecutorFactoryImpl.class).asEagerSingleton();

            bind(Context.class).toProvider(ContextProvider.class).asEagerSingleton();
            bind(Estimator.class).to(EstimatorImpl.class).asEagerSingleton();
            bind(Adviser.class).to(AdviserImpl.class).asEagerSingleton();
            bind(Instructor.class).to(InstructorImpl.class).asEagerSingleton();

            bind(Agent.class).to(AgentImpl.class).asEagerSingleton();
            bind(Pipeline.class).to(PipelineImpl.class).asEagerSingleton();
            bind(Trader.class).to(traderClass).asEagerSingleton();

            bind(Cryptotrader.class).to(CryptotraderImpl.class);

        }

    }

    private final Injector injector;

    @Inject
    public CryptotraderImpl(Injector injector) {
        this.injector = injector;
    }

    @Override
    public void execute() {

        Trader trader = injector.getInstance(Trader.class);

        log.info("Executing : {}", trader);

        trader.trade();

        log.info("Executed.");

    }

    @Override
    public void shutdown() {

        closeQuietly(Trader.class);

        closeQuietly(Bitflyer4j.class);

        closeQuietly(ExecutorFactory.class);

        log.info("Shutdown.");

    }

    @VisibleForTesting
    void closeQuietly(Class<? extends AutoCloseable> clazz) {

        try {

            AutoCloseable c = injector.getInstance(clazz);

            if (c == null) {
                return;
            }

            c.close();

        } catch (Exception e) {
            log.warn("Failed to close : " + clazz, e);
        }

    }

}
