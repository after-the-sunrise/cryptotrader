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

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

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

            return emptyMap();

        }

        OrderManager manager = managers.get(req.getSite());

        if (manager == null) {

            log.debug("Service not found for site : {}", req.getSite());

            return emptyMap();

        }

        List<Instruction> values = ofNullable(instructions).orElse(emptyList());

        if (!propertyManager.getTradingActive()) {

            log.debug("Skipping manage : {}", values.size());

            return emptyMap();

        }

        Map<Instruction, String> results = ofNullable(manager.manage(ctx, req, values)).orElse(emptyMap());

        log.debug("Managed : {}", results.size());

        results.forEach((k, v) -> log.debug("id=[{}] {}", v, k));

        return results;

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

        // TODO : Change to Map<Instruction|String, Boolean>

        Boolean result = manager.reconcile(ctx, req, instructions);

        log.debug("Reconciled : {}", result);

        return result;

    }

}
