package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.Visitor;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.*;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.commons.lang3.math.NumberUtils.LONG_ONE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class TemplateAgent implements Agent {

    private static final Duration INTERVAL = Duration.ofSeconds(LONG_ONE);

    private static final long RETRY = MINUTES.toMillis(LONG_ONE) / INTERVAL.toMillis();

    private final String id;

    public TemplateAgent(String id) {
        this.id = id;
    }

    @Override
    public String get() {
        return id;
    }

    @Override
    public Map<Instruction, String> manage(Context context, Request request, List<Instruction> instructions) {

        if (CollectionUtils.isEmpty(instructions)) {

            log.trace("Nothing to manage.");

            return Collections.emptyMap();

        }

        Set<CreateInstruction> creates = new HashSet<>();
        Set<CancelInstruction> cancels = new HashSet<>();

        Instruction.Visitor<Boolean> visitor = new Visitor<Boolean>() {
            @Override
            public Boolean visit(CreateInstruction instruction) {
                return creates.add(instruction);
            }

            @Override
            public Boolean visit(CancelInstruction instruction) {
                return cancels.add(instruction);
            }
        };

        instructions.stream().filter(Objects::nonNull).forEach(i -> i.accept(visitor));

        Key key = Key.from(request);

        Map<Instruction, String> results = new IdentityHashMap<>();

        results.putAll(context.cancelOrders(key, cancels));

        if (results.values().stream().anyMatch(StringUtils::isEmpty)) {

            log.trace("Skipping create instructions : {}", creates.size());

            return results;

        }

        results.putAll(context.createOrders(key, creates));

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
                            return checkCreated(context, key, entry.getValue(), RETRY, INTERVAL);
                        }

                        @Override
                        public Boolean visit(CancelInstruction instruction) {
                            return checkCancelled(context, key, entry.getValue(), RETRY, INTERVAL);
                        }
                    });

                    log.trace("Reconciled : {} - {}", matched, instruction);

                    results.put(instruction, matched);

                });

        return results;

    }

    @VisibleForTesting
    Boolean checkCreated(Context context, Key key, String id, long retry, Duration interval) {

        for (long i = 0; i <= retry; i++) {

            Order order = context.findOrder(key, id);

            if (order != null) {

                log.trace("Reconcile create succeeded : {}", id);

                return TRUE;

            }

            try {

                Thread.sleep(interval.toMillis());

            } catch (InterruptedException e) {

                log.trace("Reconciling create interrupted : {}", id);

                return FALSE;

            }

        }

        log.trace("Reconcile create failed : {}", id);

        return FALSE;

    }

    @VisibleForTesting
    Boolean checkCancelled(Context context, Key key, String id, long retry, Duration interval) {

        for (long i = 0; i <= retry; i++) {

            Order order = context.findOrder(key, id);

            if (order == null || !TRUE.equals(order.getActive())) {

                log.trace("Reconcile cancel succeeded : {}", id);

                return TRUE;

            }

            try {

                Thread.sleep(interval.toMillis());

            } catch (InterruptedException e) {

                log.trace("Reconciling cancel interrupted : {}", id);

                return FALSE;

            }

        }

        log.trace("Reconcile cancel failed : {}", id);

        return FALSE;

    }

}
