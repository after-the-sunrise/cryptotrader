package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.OrderManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.Boolean.FALSE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class OrderManagerImpl implements OrderManager {

    private final Map<String, OrderManager> managers;

    @Inject
    public OrderManagerImpl(Injector injector) {

        this.managers = injector.getInstance(ServiceFactory.class).loadMap(OrderManager.class);

    }

    @Override
    public String get() {
        return Request.ALL;
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

    private Boolean forRequest(Request request, Function<OrderManager, Boolean> function) {

        if (!Request.isValid(request)) {

            log.trace("Invalid request : {}", request);

            return FALSE;

        }

        OrderManager manager = managers.get(request.getSite());

        if (manager == null) {

            log.debug("OrderManager not found for site : {}", request.getSite());

            return FALSE;

        }

        return Optional.ofNullable(function.apply(manager)).orElse(FALSE);

    }

}
