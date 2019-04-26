package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.StateType;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.Visitor;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.impl.AbstractService;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class TemplateAgent extends AbstractService implements Agent {

    static final Duration INTERVAL = Duration.ofSeconds(5);

    private final String id;

    public TemplateAgent(String id) {
        this.id = id;
    }

    @Override
    public String get() {
        return id;
    }

    @VisibleForTesting
    Instant getNow() {
        return Instant.now();
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

        } else {

            results.putAll(context.createOrders(key, creates));

        }

        return results;

    }

    @Override
    public Map<Instruction, Boolean> reconcile(Context context, Request request, Map<Instruction, String> instructions) {

        if (MapUtils.isEmpty(instructions)) {

            log.trace("Nothing to reconcile.");

            return Collections.emptyMap();

        }

        if (Boolean.valueOf(getStringProperty("shortcut", "false"))) {

            log.trace("Skipping reconcile.");

            return Collections.emptyMap();

        }

        Map<String, CreateInstruction> creates = new HashMap<>();
        Map<String, CancelInstruction> cancels = new HashMap<>();

        trimToEmpty(instructions).entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getKey()))
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .forEach(entry -> entry.getKey().accept(new Visitor<Instruction>() {
                            @Override
                            public Instruction visit(CreateInstruction instruction) {
                                return creates.put(entry.getValue(), instruction);
                            }

                            @Override
                            public Instruction visit(CancelInstruction instruction) {
                                return cancels.put(entry.getValue(), instruction);
                            }
                        })
                );

        Map<String, Instruction> remaining = new HashMap<>();
        remaining.putAll(creates);
        remaining.putAll(cancels);

        Map<Instruction, Boolean> results = new IdentityHashMap<>();

        Key key = Key.from(request);

        while (!remaining.isEmpty()) {

            key = nextKey(key, Duration.ofMillis(getLongProperty("interval", INTERVAL.toMillis())));

            for (Entry<String, Instruction> entry : new HashMap<>(remaining).entrySet()) {

                if (key != null && key.getTimestamp().isBefore(request.getTargetTime()) && context.getState(key) != StateType.TERMINATE) {

                    Order order = context.findOrder(key, entry.getKey());

                    if (creates.containsKey(entry.getKey())) {

                        if (order != null) {

                            remaining.remove(entry.getKey());

                            results.put(entry.getValue(), TRUE);

                        }

                        continue;

                    }

                    if (cancels.containsKey(entry.getKey())) {

                        if (order == null || !TRUE.equals(order.getActive())) {

                            remaining.remove(entry.getKey());

                            results.put(entry.getValue(), TRUE);

                        }

                        continue;

                    }

                }

                remaining.remove(entry.getKey());

                results.put(entry.getValue(), FALSE);

            }


        }

        return results;

    }

    @VisibleForTesting
    Key nextKey(Key current, Duration interval) {

        Key next = null;

        try {

            MILLISECONDS.sleep(interval.toMillis());

            next = Key.build(current).timestamp(getNow()).build();

        } catch (InterruptedException e) {

            log.trace("Reconciling cancel interrupted.");

            Thread.currentThread().interrupt();

        }

        return next;

    }

}
