package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static java.math.RoundingMode.DOWN;
import static java.math.RoundingMode.UP;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class TemplateInstructor implements Instructor {

    private static final List<Order> EMPTY = emptyList();

    private static final int SCALE = 12;

    private static final BigDecimal INCREMENT = ONE.movePointLeft(SCALE);

    private final String id;

    public TemplateInstructor(String id) {
        this.id = id;
    }

    @Override
    public String get() {
        return id;
    }

    @Override
    public List<Instruction> instruct(Context context, Request request, Adviser.Advice advice) {

        if (!Request.isValid(request)) {

            log.trace("Invalid request : {}", request);

            return Collections.emptyList();

        }

        if (advice == null) {

            log.trace("Invalid advice : {}", request);

            return Collections.emptyList();

        }

        List<Instruction.CreateInstruction> creates = new ArrayList<>();

        creates.addAll(createBuys(context, request, advice));

        creates.addAll(createSells(context, request, advice));

        return merge(creates, createCancels(context, request));

    }

    @VisibleForTesting
    Map<Instruction.CancelInstruction, Order> createCancels(Context context, Request request) {

        Key key = Key.from(request);

        List<Order> orders = ofNullable(context.listOrders(key)).orElse(EMPTY);

        Map<Instruction.CancelInstruction, Order> cancels = new IdentityHashMap<>();

        orders.forEach(o -> cancels.put(Instruction.CancelInstruction.builder().id(o.getId()).build(), o));

        log.trace("Cancel candidates : {}", cancels.keySet());

        return cancels;

    }

    @VisibleForTesting
    List<Instruction.CreateInstruction> createBuys(Context context, Request request, Adviser.Advice adv) {

        if (adv.getBuyLimitPrice() == null || adv.getBuyLimitPrice().signum() == 0) {
            return Collections.emptyList();
        }

        if (adv.getBuyLimitSize() == null || adv.getBuyLimitSize().signum() <= 0) {
            return Collections.emptyList();
        }

        List<BigDecimal> s = splitInstrumentSize(context, request, adv.getBuyLimitSize());

        List<BigDecimal> p = splitInstrumentPrice(context, request, adv.getBuyLimitPrice(), s.size(), INCREMENT.negate());

        List<Instruction.CreateInstruction> instructions = new ArrayList<>(s.size());

        for (int i = 0; i < s.size(); i++) {
            instructions.add(Instruction.CreateInstruction.builder().price(p.get(i)).size(s.get(i)).build());
        }

        log.trace("Buy candidates : {}", instructions);

        return instructions;

    }

    @VisibleForTesting
    List<Instruction.CreateInstruction> createSells(Context context, Request request, Adviser.Advice adv) {

        if (adv.getSellLimitPrice() == null || adv.getSellLimitPrice().signum() == 0) {
            return Collections.emptyList();
        }

        if (adv.getSellLimitSize() == null || adv.getSellLimitSize().signum() <= 0) {
            return Collections.emptyList();
        }

        List<BigDecimal> s = splitInstrumentSize(context, request, adv.getSellLimitSize());

        List<BigDecimal> p = splitInstrumentPrice(context, request, adv.getSellLimitPrice(), s.size(), INCREMENT);

        List<Instruction.CreateInstruction> instructions = new ArrayList<>(s.size());

        for (int i = 0; i < s.size(); i++) {

            BigDecimal size = s.get(i) == null ? null : s.get(i).negate();

            instructions.add(Instruction.CreateInstruction.builder().price(p.get(i)).size(size).build());

        }

        log.trace("Sell candidates : {}", instructions);

        return instructions;

    }

    @VisibleForTesting
    List<BigDecimal> splitInstrumentSize(Context context, Request request, BigDecimal value) {

        BigDecimal base = value;

        if (base.signum() <= 0) {
            return singletonList(value);
        }

        int offset = 0;

        while (base.compareTo(ONE) < 0) {
            base = base.movePointRight(1);
            offset--;
        }

        while (base.compareTo(TEN) > 0) {
            base = base.movePointLeft(1);
            offset++;
        }

        Key key = Key.from(request);

        BigDecimal scaledBase = base.setScale(INTEGER_ZERO, DOWN).scaleByPowerOfTen(offset);

        BigDecimal split = request.getTradingSplit().setScale(INTEGER_ZERO, DOWN);

        BigDecimal slice = scaledBase.divide(split, SCALE, DOWN);

        BigDecimal rounded = context.roundLotSize(key, slice, DOWN);

        if (rounded == null || rounded.signum() <= 0) {
            return singletonList(value);
        }

        return Collections.nCopies(split.intValue(), rounded);

    }

    List<BigDecimal> splitInstrumentPrice(Context context, Request request, BigDecimal value, int size, BigDecimal delta) {

        List<BigDecimal> values = new ArrayList<>(size);

        BigDecimal previous = value;

        Key key = Key.from(request);

        for (int i = 0; i < size; i++) {

            BigDecimal raw = previous == null ? null : previous.add(delta);

            BigDecimal rounded = context.roundTickSize(key, raw, delta.signum() >= 0 ? UP : DOWN);

            values.add(rounded == null ? previous : rounded);

            previous = rounded;

        }

        return values;

    }

    @VisibleForTesting
    List<Instruction> merge(List<Instruction.CreateInstruction> creates, Map<Instruction.CancelInstruction, Order> cancels) {

        Map<Instruction.CancelInstruction, Order> remainingCancels = new IdentityHashMap<>(cancels);

        List<Instruction.CreateInstruction> remainingCreates = new ArrayList<>(creates);

        Iterator<Instruction.CreateInstruction> createItr = remainingCreates.iterator();

        while (createItr.hasNext()) {

            Instruction.CreateInstruction create = createItr.next();

            Iterator<Map.Entry<Instruction.CancelInstruction, Order>> cancelItr = remainingCancels.entrySet().iterator();

            while (cancelItr.hasNext()) {

                Map.Entry<Instruction.CancelInstruction, Order> entry = cancelItr.next();

                if (entry.getValue().getOrderPrice() == null) {
                    continue;
                }

                if (create.getPrice().compareTo(entry.getValue().getOrderPrice()) != 0) {
                    continue;
                }

                if (entry.getValue().getRemainingQuantity() == null) {
                    continue;
                }

                if (entry.getValue().getRemainingQuantity().compareTo(create.getSize()) != 0) {
                    continue;
                }

                log.trace("Netting cancel/create : {} - {}", create, entry.getKey());

                cancelItr.remove();

                createItr.remove();

                break;

            }

        }

        List<Instruction> instructions = new ArrayList<>();
        instructions.addAll(remainingCancels.keySet());
        instructions.addAll(remainingCreates);
        return instructions;

    }

}
