package com.after_sunrise.cryptocurrency.cryptotrader.web;

import com.after_sunrise.cryptocurrency.cryptotrader.Cryptotrader;
import com.after_sunrise.cryptocurrency.cryptotrader.core.ConfigurationProvider;
import com.after_sunrise.cryptocurrency.cryptotrader.core.CryptotraderImpl;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration2.Configuration;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;

import javax.annotation.PreDestroy;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

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

        private static final DateTimeFormatter DTF = ISO_INSTANT;

        private static final Instant LAUNCH_TIME = Instant.now();

        private final AtomicReference<Instant> CONFIG_TIME = new AtomicReference<>(LAUNCH_TIME);

        private final Gson gson;

        private final Trader trader;

        private final ConfigurationProvider configurationProvider;

        @Inject
        public EndpointImpl(Injector injector) {

            this.gson = new Gson();

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

            CONFIG_TIME.set(Instant.now());

        }

        @GET
        @Path("/configuration")
        @Produces(MediaType.APPLICATION_JSON)
        public String getConfiguration() {

            Configuration c = configurationProvider.get();

            Collection<String> keys = new TreeSet<>();

            CollectionUtils.addAll(keys, c.getKeys());

            StringBuilder sb = new StringBuilder();

            for (String key : keys) {
                sb.append(key);
                sb.append('=');
                sb.append(c.getString(key));
                sb.append(System.lineSeparator());
            }

            String data = sb.toString();

            Map<String, Object> map = new TreeMap<>();
            map.put("data", data);
            map.put("size", data.length());
            map.put("hash", DigestUtils.md5Hex(data));
            return gson.toJson(map);

        }

        @GET
        @Path("/time/launch")
        @Produces(MediaType.APPLICATION_JSON)
        public String getLaunchTime() {
            return gson.toJson(Collections.singletonMap("time", DTF.format(LAUNCH_TIME)));
        }

        @GET
        @Path("/time/config")
        @Produces(MediaType.APPLICATION_JSON)
        public String getConfigTime() {
            return gson.toJson(Collections.singletonMap("time", DTF.format(CONFIG_TIME.get())));
        }

    }

}
