package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.OrderManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class AgentImpl implements OrderManager {

    private static final List<Order> EMTPY = Collections.emptyList();

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
    public Map<Instruction, String> manage(Context ctx, Request req, List<Instruction> vals) {

        if (Request.isInvalid(req)) {

            log.trace("Invalid request : {}", req);

            return Collections.emptyMap();

        }

        OrderManager manager = managers.get(req.getSite());

        if (manager == null) {

            log.debug("OrderManager not found for site : {}", req.getSite());

            return Collections.emptyMap();

        }

        if (!propertyManager.getTradingActive()) {

            vals.forEach(i -> log.debug("Skipped by dry-run : {} - {}", i, req));

            return Collections.emptyMap();

        }

        return manager.manage(ctx, req, vals);

    }

    @Override
    public Boolean reconcile(Context ctx, Request req, Map<Instruction, String> instructions) {

        if (Request.isInvalid(req)) {

            log.trace("Invalid request : {}", req);

            return Boolean.FALSE;

        }

        OrderManager manager = managers.get(req.getSite());

        if (manager == null) {

            log.debug("OrderManager not found for site : {}", req.getSite());

            return Boolean.FALSE;

        }

        if (!propertyManager.getTradingActive()) {

            instructions.forEach((k, v) -> log.debug("Skipped by dry-run : {} - {}", k, req));

            return Boolean.FALSE;

        }

        return manager.reconcile(ctx, req, instructions);

    }

}
