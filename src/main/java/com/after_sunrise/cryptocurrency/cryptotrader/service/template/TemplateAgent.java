package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.Visitor;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.OrderManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class TemplateAgent implements OrderManager {

    private final String id;

    public TemplateAgent(String id) {
        this.id = id;
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
    public Boolean reconcile(Context context, Request request, Map<Instruction, String> instructions) {

        if (MapUtils.isEmpty(instructions)) {

            log.trace("Nothing to reconcile.");

            return Boolean.TRUE;

        }

        Key key = Key.from(request);

        return instructions.entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getKey()))
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .map(entry -> {

                    Instruction instruction = entry.getKey();

                    String id = entry.getValue();

                    Boolean matched = instruction.accept(new Visitor<Boolean>() {
                        @Override
                        public Boolean visit(CreateInstruction instruction) {

                            return checkCreated(context, key, id);

                        }

                        @Override
                        public Boolean visit(CancelInstruction instruction) {

                            return checkCancelled(context, key, id);

                        }
                    });

                    return matched;

                }).allMatch(Boolean.TRUE::equals);

    }

    @VisibleForTesting
    Boolean checkCreated(Context context, Key key, String id) {

        // TODO : Retry

        Boolean result = context.findOrder(key, id) != null;

        log.trace("Create check : {} - {}", result, id);

        return result;

    }

    @VisibleForTesting
    Boolean checkCancelled(Context context, Key key, String id) {

        // TODO : Retry

        Order order = context.findOrder(key, id);

        Boolean result = order == null || !Boolean.TRUE.equals(order.getActive());

        log.trace("Cancel check : {} - {}", result, id);

        return result;

    }

}
