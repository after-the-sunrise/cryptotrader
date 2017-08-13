package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.OrderManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.function.Function;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class OrderManagerImpl implements OrderManager {

    private final Map<String, OrderManager> managers;

    @Inject
    public OrderManagerImpl(Injector injector) {

        this.managers = Frameworks.loadMap(OrderManager.class, injector);

    }

    @Override
    public String get() {
        return Request.ALL;
    }

    @VisibleForTesting
    <V> V forRequest(Request request, Function<OrderManager, V> function) {

        if (Frameworks.isInvalid(request)) {

            log.trace("Invalid request : {}", request);

            return null;

        }

        OrderManager manager = managers.get(request.getSite());

        if (manager == null) {

            log.debug("OrderManager not found for site : {}", request.getSite());

            return null;

        }

        return function.apply(manager);

    }

    @Override
    public Boolean create(Request request, CreateInstruction instruction) {

        log.debug("Order instruction : {}", instruction);

        return forRequest(request, m -> m.create(request, instruction));

    }

    @Override
    public Boolean cancel(Request request, CancelInstruction instruction) {

        log.debug("Order instruction : {}", instruction);

        return forRequest(request, m -> m.cancel(request, instruction));

    }

}
