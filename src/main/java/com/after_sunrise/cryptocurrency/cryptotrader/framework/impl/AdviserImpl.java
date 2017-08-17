package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
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
public class AdviserImpl implements Adviser {

    private final Advice BAIL = Advice.builder().build();

    private final Map<String, Adviser> advisers;

    @Inject
    public AdviserImpl(Injector injector) {

        this.advisers = injector.getInstance(ServiceFactory.class).loadMap(Adviser.class);

    }

    @Override
    public String get() {
        return Trader.Request.ALL;
    }

    @Override
    public Advice advise(Context context, Request request, Estimation estimation) {

        if (Request.isInvalid(request)) {

            log.trace("Invalid request : {}", request);

            return BAIL;

        }

        Adviser adviser = advisers.get(request.getSite());

        if (adviser == null) {

            log.debug("Adviser not found : {}", request);

            return BAIL;

        }

        Advice advice = adviser.advise(context, request, estimation);

        log.debug("Advised : {}", advice);

        return Optional.ofNullable(advice).orElse(BAIL);

    }

}
