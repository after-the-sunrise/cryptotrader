package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.OrderManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class AgentImpl implements OrderManager {

    private final PropertyManager propertyManager;

    private final Map<String, OrderManager> managers;

    @Inject
    public AgentImpl(Injector injector) {

        this.propertyManager = injector.getInstance(PropertyManager.class);

        this.managers = injector.getInstance(ServiceFactory.class).loadMap(OrderManager.class);

    }

    @Override
    public String get() {
        return Request.ALL;
    }

    @Override
    public Map<Instruction, String> manage(Context ctx, Request req, List<Instruction> instructions) {

        if (Request.isInvalid(req)) {

            log.trace("Invalid request : {}", req);

            return Collections.emptyMap();

        }

        OrderManager manager = managers.get(req.getSite());

        if (manager == null) {

            log.debug("Service not found for site : {}", req.getSite());

            return Collections.emptyMap();

        }

        List<Instruction> values = Optional.ofNullable(instructions).orElse(Collections.emptyList());

        if (!propertyManager.getTradingActive()) {

            log.debug("Skipping manage : {}", values.size());

            return Collections.emptyMap();

        }

        log.debug("Managing : {}", values.size());

        values.forEach(i -> log.debug("{}", i));

        return manager.manage(ctx, req, values);

    }

    @Override
    public Boolean reconcile(Context ctx, Request req, Map<Instruction, String> instructions) {

        if (Request.isInvalid(req)) {

            log.trace("Invalid request : {}", req);

            return Boolean.FALSE;

        }

        OrderManager manager = managers.get(req.getSite());

        if (manager == null) {

            log.debug("Service not found for site : {}", req.getSite());

            return Boolean.FALSE;

        }

        Map<Instruction, String> values = Optional.ofNullable(instructions).orElse(Collections.emptyMap());

        log.debug("Reconciling : {}", values.size());

        values.forEach((k, v) -> log.debug("[{}] {}", v, k));

        return manager.reconcile(ctx, req, values);

    }

}
