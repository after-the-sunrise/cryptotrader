package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instructor;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.*;

import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.DOWN;
import static java.math.RoundingMode.UP;
import static java.util.Collections.emptyList;
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

    private static final BigDecimal EPSILON = ONE.movePointLeft(SCALE);

    private final String id;

    public TemplateInstructor(String id) {
        this.id = id;
    }

    @Override
    public String get() {
        return id;
    }

    @Override
    public List<Instruction> instruct(Context context, Request request, Advice advice) {

        List<CreateInstruction> creates = new ArrayList<>();

        creates.addAll(createBuys(context, request, advice));

        creates.addAll(createSells(context, request, advice));

        return merge(creates, createCancels(context, request));

    }

    @VisibleForTesting
    Map<CancelInstruction, Order> createCancels(Context context, Request request) {

        Key key = Key.from(request);

        List<Order> orders = ofNullable(context.listActiveOrders(key)).orElse(EMPTY);

        Map<CancelInstruction, Order> cancels = new IdentityHashMap<>();

        orders.stream()
                .filter(Objects::nonNull)
                .filter(o -> StringUtils.isNotEmpty(o.getId()))
                .filter(o -> TRUE.equals(o.getActive()))
                .forEach(o -> cancels.put(CancelInstruction.builder().id(o.getId()).build(), o));

        cancels.forEach((k, v) -> log.trace("Cancel candidate : {}", v));

        return cancels;

    }

    @VisibleForTesting
    List<CreateInstruction> createBuys(Context context, Request request, Advice adv) {

        List<BigDecimal> s = splitSize(context, request, adv.getBuyLimitSize());

        List<BigDecimal> p = splitPrice(context, request, adv.getBuyLimitPrice(), s.size(), EPSILON.negate());

        List<CreateInstruction> instructions = new ArrayList<>(s.size());

        for (int i = 0; i < s.size(); i++) {
            instructions.add(CreateInstruction.builder().price(p.get(i)).size(s.get(i)).build());
        }

        instructions.forEach((v) -> log.trace("Buy candidate : {}", v));

        return instructions;

    }

    @VisibleForTesting
    List<CreateInstruction> createSells(Context context, Request request, Advice adv) {

        List<BigDecimal> s = splitSize(context, request, adv.getSellLimitSize());

        List<BigDecimal> p = splitPrice(context, request, adv.getSellLimitPrice(), s.size(), EPSILON);

        List<CreateInstruction> instructions = new ArrayList<>(s.size());

        for (int i = 0; i < s.size(); i++) {

            BigDecimal size = s.get(i) == null ? null : s.get(i).negate();

            instructions.add(CreateInstruction.builder().price(p.get(i)).size(size).build());

        }

        instructions.forEach((v) -> log.trace("Sell candidate : {}", v));

        return instructions;

    }

    private List<BigDecimal> splitSize(Context context, Request request, BigDecimal value) {

        Key key = Key.from(request);

        BigDecimal total = context.roundLotSize(key, value, DOWN);

        if (total == null || total.signum() <= 0) {
            return emptyList();
        }

        BigDecimal unit = Optional.ofNullable(context.roundLotSize(key, EPSILON, UP)).orElse(total);

        BigDecimal split = request.getTradingSplit().setScale(INTEGER_ZERO, DOWN).max(ONE);

        BigDecimal fair = total.divide(split, SCALE, DOWN);

        BigDecimal floor = Optional.ofNullable(context.roundLotSize(key, fair, DOWN)).orElse(ZERO);

        BigDecimal residual = total.subtract(split.multiply(floor));

        List<BigDecimal> results = new ArrayList<>(split.intValue());

        for (int i = 0; i < split.intValue(); i++) {

            BigDecimal slice = floor;

            if (residual.compareTo(unit) >= 0) {

                slice = slice.add(unit);

                residual = residual.subtract(unit);

            }

            if (slice.signum() <= 0) {
                continue;
            }

            results.add(slice);

        }

        return results;

    }

    private List<BigDecimal> splitPrice(Context context, Request request, BigDecimal value, int size, BigDecimal delta) {

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
    List<Instruction> merge(List<CreateInstruction> creates, Map<CancelInstruction, Order> cancels) {

        Map<CancelInstruction, Order> remainingCancels = new IdentityHashMap<>(cancels);

        List<CreateInstruction> remainingCreates = new ArrayList<>(creates);

        Iterator<CreateInstruction> createItr = remainingCreates.iterator();

        while (createItr.hasNext()) {

            CreateInstruction create = createItr.next();

            Iterator<Map.Entry<CancelInstruction, Order>> cancelItr = remainingCancels.entrySet().iterator();

            while (cancelItr.hasNext()) {

                Map.Entry<CancelInstruction, Order> entry = cancelItr.next();

                if (isDifferent(entry.getValue().getOrderPrice(), create.getPrice())) {
                    continue;
                }

                if (isDifferent(entry.getValue().getRemainingQuantity(), create.getSize())) {
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

        instructions.forEach(v -> log.trace("Merged candidate : {}", v));

        return instructions;

    }

    private boolean isDifferent(BigDecimal v1, BigDecimal v2) {
        return v1 == null || v2 == null || v1.compareTo(v2) != 0;
    }

}
