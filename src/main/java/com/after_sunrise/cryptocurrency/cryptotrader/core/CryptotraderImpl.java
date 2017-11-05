package com.after_sunrise.cryptocurrency.cryptotrader.core;

import com.after_sunrise.cryptocurrency.cryptotrader.Cryptotrader;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.impl.*;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ImmutableConfiguration;


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
            bind(ImmutableConfiguration.class).to(Configuration.class).asEagerSingleton();
            bind(PropertyController.class).to(PropertyManagerImpl.class).asEagerSingleton();
            bind(PropertyManager.class).to(PropertyController.class).asEagerSingleton();
            bind(ServiceFactory.class).to(ServiceFactoryImpl.class).asEagerSingleton();
            bind(ExecutorFactory.class).to(ExecutorFactoryImpl.class).asEagerSingleton();

            bind(Context.class).to(ContextImpl.class).asEagerSingleton();
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

        log.info("Shutting down...");

        closeQuietly(Trader.class);

        closeQuietly(Context.class);

        closeQuietly(ExecutorFactory.class);

        log.info("Shutdown complete.");

    }

    private void closeQuietly(Class<? extends AutoCloseable> clazz) {

        try {

            AutoCloseable c = injector.getInstance(clazz);

            c.close();

        } catch (Exception e) {
            log.warn("Failed to close : " + clazz, e);
        }

    }

}
