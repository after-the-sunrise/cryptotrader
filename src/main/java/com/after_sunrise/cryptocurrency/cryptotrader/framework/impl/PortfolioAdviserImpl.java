package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.MarketEstimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.PortfolioAdviser;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class PortfolioAdviserImpl implements PortfolioAdviser {

    private final Advice BAIL = Advice.builder().build();

    private final Map<String, PortfolioAdviser> advisers;

    @Inject
    public PortfolioAdviserImpl(Injector injector) {

        this.advisers = Frameworks.loadMap(PortfolioAdviser.class, injector);

    }

    @Override
    public String get() {
        return Trader.Request.ALL;
    }

    @Override
    public Advice advise(Context context, Request request, Estimation estimation) {

        if (Frameworks.isInvalid(request)) {

            log.trace("Invalid request : {}", request);

            return BAIL;

        }

        PortfolioAdviser adviser = advisers.get(request.getSite());

        if (adviser == null) {

            log.debug("Adviser not found : {}", request);

            return BAIL;

        }

        Advice advice = adviser.advise(context, request, estimation);

        log.debug("Advised : {}", advice);

        return Optional.ofNullable(advice).orElse(BAIL);

    }

}
