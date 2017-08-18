package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Agent;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.Visitor;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.*;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class TemplateAgent implements Agent {

    private static final int LIMIT = 10;

    private static final long INTERVAL = SECONDS.toMillis(6L);

    private final String id;

    private final int retryLimit;

    private final long retryInterval;

    public TemplateAgent(String id) {
        this(id, LIMIT, INTERVAL);
    }

    public TemplateAgent(String id, int retryLimit, long retryInterval) {
        this.id = id;
        this.retryLimit = retryLimit;
        this.retryInterval = retryInterval;
    }

    @Override
    public String get() {
        return id;
    }

    @Override
    public Map<Instruction, String> manage(Context ctx, Request req, List<Instruction> vals) {

        if (CollectionUtils.isEmpty(vals)) {

            log.trace("Nothing to manage.");

            return Collections.emptyMap();

        }

        Key key = Key.from(req);

        Map<Instruction, String> results = new IdentityHashMap<>();

        vals.stream()
                .filter(Objects::nonNull)
                .forEach(i -> i.accept(new Visitor<Void>() {
                    @Override
                    public Void visit(CreateInstruction instruction) {

                        log.trace("Creating : {} - {}", key, instruction);

                        results.put(instruction, ctx.createOrder(key, instruction));

                        return null;

                    }

                    @Override
                    public Void visit(CancelInstruction instruction) {

                        log.trace("Cancelling : {} - {}", key, instruction);

                        results.put(instruction, ctx.cancelOrder(key, instruction));

                        return null;

                    }
                }));

        return results;

    }

    @Override
    public Map<Instruction, Boolean> reconcile(Context context, Request request, Map<Instruction, String> instructions) {

        if (MapUtils.isEmpty(instructions)) {

            log.trace("Nothing to reconcile.");

            return Collections.emptyMap();

        }

        Key key = Key.from(request);

        Map<Instruction, Boolean> results = new IdentityHashMap<>();

        instructions.entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getKey()))
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .forEach(entry -> {

                    Instruction instruction = entry.getKey();

                    Boolean matched = instruction.accept(new Visitor<Boolean>() {
                        @Override
                        public Boolean visit(CreateInstruction instruction) {

                            return checkCreated(context, key, entry.getValue());

                        }

                        @Override
                        public Boolean visit(CancelInstruction instruction) {

                            return checkCancelled(context, key, entry.getValue());

                        }
                    });

                    log.trace("Reconciled : {} - {}", matched, instruction);

                    results.put(instruction, matched);

                });

        return results;

    }

    @VisibleForTesting
    Boolean checkCreated(Context context, Key key, String id) {

        for (int i = 0; i < retryLimit; i++) {

            log.trace("Reconciling create : {}", id);

            Order order = context.findOrder(key, id);

            if (order != null) {

                log.trace("Reconcile create succeeded : {}", id);

                return TRUE;

            }

            try {

                Thread.sleep(retryInterval);

            } catch (InterruptedException e) {

                log.trace("Reconciling create interrupted : {}", id);

                return FALSE;

            }

        }

        log.trace("Reconcile create failed : {}", id);

        return FALSE;

    }

    @VisibleForTesting
    Boolean checkCancelled(Context context, Key key, String id) {

        for (int i = 0; i < retryLimit; i++) {

            log.trace("Reconciling cancel : {}", id);

            Order order = context.findOrder(key, id);

            if (order == null || !TRUE.equals(order.getActive())) {

                log.trace("Reconcile cancel succeeded : {}", id);

                return TRUE;

            }

            try {

                Thread.sleep(retryInterval);

            } catch (InterruptedException e) {

                log.trace("Reconciling cancel interrupted : {}", id);

                return FALSE;

            }

        }

        log.trace("Reconcile cancel failed : {}", id);

        return FALSE;

    }

}
