package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Agent;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.google.inject.Inject;
import com.google.inject.Injector;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class AgentImpl extends AbstractService implements Agent {

    private final PropertyManager propertyManager;

    private final Map<String, Agent> managers;

    @Inject
    public AgentImpl(Injector injector) {

        this.propertyManager = injector.getInstance(PropertyManager.class);

        this.managers = injector.getInstance(ServiceFactory.class).loadMap(Agent.class);

    }

    @Override
    public String get() {
        return WILDCARD;
    }

    @Override
    public Map<Instruction, String> manage(Context ctx, Request req, List<Instruction> instructions) {

        Agent manager = managers.get(req.getSite());

        if (manager == null) {

            log.debug("Service not found for site : {}", req.getSite());

            return emptyMap();

        }

        List<Instruction> values = ofNullable(instructions).orElse(emptyList());

        if (!propertyManager.getTradingActive(req.getSite(), req.getInstrument())) {

            log.debug("Skipping manage : {}", values.size());

            return emptyMap();

        }

        Map<Instruction, String> results = ofNullable(manager.manage(ctx, req, values)).orElse(emptyMap());

        log.info("Manage : [{}.{}] {}", req.getSite(), req.getInstrument(), results.size());

        results.forEach((k, v) -> log.debug("id=[{}] {}", v, k));

        return results;

    }

    @Override
    public Map<Instruction, Boolean> reconcile(Context ctx, Request req, Map<Instruction, String> instructions) {

        Agent manager = managers.get(req.getSite());

        if (manager == null) {

            log.debug("Service not found for site : {}", req.getSite());

            return emptyMap();

        }

        Map<Instruction, Boolean> results = ofNullable(manager.reconcile(ctx, req, instructions)).orElse(emptyMap());

        log.info("Reconcile : [{}.{}] {}", req.getSite(), req.getInstrument(), results.size());

        results.forEach((k, v) -> log.debug("Reconcile=[{}] {}", v, k));

        return results;

    }

}
