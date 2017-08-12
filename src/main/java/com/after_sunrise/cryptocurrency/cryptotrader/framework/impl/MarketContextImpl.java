package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.MarketEstimator.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.ServiceLoader.load;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class MarketContextImpl implements Context {

    private final Map<String, Context> contexts;

    @Inject
    public MarketContextImpl(Injector injector) {

        this.contexts = new HashMap<>();

        load(Context.class).iterator().forEachRemaining(c -> contexts.put(c.get(), c));

        this.contexts.values().forEach(injector::injectMembers);

    }

    @Override
    public String get() {
        return Trader.Request.ALL;
    }

    @VisibleForTesting
    <T> T forSite(String site, Function<Context, T> consumer) {

        Context context = contexts.get(site);

        if (context == null) {
            return null;
        }

        return consumer.apply(context);

    }

    @Override
    public BigDecimal getBesAskPrice(String site, String instrument, Instant timestamp) {
        return forSite(site, c -> c.getBesAskPrice(site, instrument, timestamp));
    }

    @Override
    public BigDecimal getBesBidPrice(String site, String instrument, Instant timestamp) {
        return forSite(site, c -> c.getBesBidPrice(site, instrument, timestamp));
    }

}
