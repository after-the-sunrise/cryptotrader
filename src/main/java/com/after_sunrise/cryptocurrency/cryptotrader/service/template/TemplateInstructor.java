package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.impl.AbstractService;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.IntStream;

import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.valueOf;
import static java.math.RoundingMode.*;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ONE;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class TemplateInstructor extends AbstractService implements Instructor {

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

        List<Order> orders = trimToEmpty(context.listActiveOrders(key));

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

        BigDecimal spread = trimToZero(adv.getBuySpread()).max(EPSILON).negate();

        List<BigDecimal> p = splitPrice(context, request, adv.getBuyLimitPrice(), s.size(), spread);

        List<CreateInstruction> instructions = new ArrayList<>(s.size());

        for (int i = 0; i < s.size(); i++) {

            BigDecimal price = p.get(i);

            BigDecimal size = s.get(i);

            instructions.add(CreateInstruction.builder().price(price).size(size).build());

        }

        instructions.forEach((v) -> log.trace("Buy candidate : {}", v));

        return instructions;

    }

    @VisibleForTesting
    List<CreateInstruction> createSells(Context context, Request request, Advice adv) {

        List<BigDecimal> s = splitSize(context, request, adv.getSellLimitSize());

        BigDecimal spread = trimToZero(adv.getSellSpread()).max(EPSILON);

        List<BigDecimal> p = splitPrice(context, request, adv.getSellLimitPrice(), s.size(), spread);

        List<CreateInstruction> instructions = new ArrayList<>(s.size());

        for (int i = 0; i < s.size(); i++) {

            BigDecimal price = p.get(i);

            BigDecimal size = s.get(i) == null ? null : s.get(i).negate();

            instructions.add(CreateInstruction.builder().price(price).size(size).build());

        }

        instructions.forEach((v) -> log.trace("Sell candidate : {}", v));

        return instructions;

    }

    @VisibleForTesting
    List<BigDecimal> splitSize(Context context, Request request, BigDecimal value) {

        Key key = Key.from(request);

        BigDecimal total = context.roundLotSize(key, value, DOWN);

        if (total == null || total.signum() <= 0) {
            return emptyList();
        }

        BigDecimal splits = valueOf(trim(request.getTradingSplit(), INTEGER_ONE)).max(ONE);

        BigDecimal lotSize = trim(context.roundLotSize(key, EPSILON, UP), total);

        BigDecimal remainingUnits = total.divide(lotSize, 0, DOWN);

        BigDecimal averageUnits = remainingUnits.divide(splits, 0, DOWN).max(ONE);

        int points = Math.max(averageUnits.precision() - 2, 0);

        BigDecimal adjustedUnits = averageUnits
                .movePointLeft(points)
                .setScale(INTEGER_ZERO, DOWN)
                .movePointRight(points);

        BigDecimal minimumQuantity = trimToZero(request.getTradingMinimum());

        BigDecimal minimumUnits = minimumQuantity.divide(lotSize, 0, UP);

        List<BigDecimal> results = new ArrayList<>(splits.intValue());

        for (int i = 0; i < splits.intValue(); i++) {

            if (remainingUnits.signum() == 0) {
                break;
            }

            if (i + 1 == splits.intValue() || adjustedUnits.signum() == 0) {

                results.add(remainingUnits.multiply(lotSize));

                break;

            }

            if (remainingUnits.subtract(minimumUnits).subtract(minimumUnits).signum() < 0) {

                results.add(remainingUnits.multiply(lotSize));

                break;

            }

            BigDecimal currentUnits = adjustedUnits.max(minimumUnits).min(remainingUnits);

            results.add(currentUnits.multiply(lotSize));

            remainingUnits = remainingUnits.subtract(currentUnits);

        }

        return results;

    }

    private List<BigDecimal> splitPrice(Context c, Request r, BigDecimal base, int splits, BigDecimal basis) {

        if (base == null) {
            return IntStream.range(0, splits).mapToObj(i -> (BigDecimal) null).collect(toList());
        }

        Key key = Key.from(r);

        List<BigDecimal> values = new ArrayList<>(splits);

        BigDecimal deltaBasis = trimToZero(basis).divide(valueOf(splits - 1).max(ONE), SCALE, HALF_UP);

        BigDecimal deltaAmount = base.multiply(deltaBasis);

        RoundingMode mode = trimToZero(basis).signum() >= 0 ? UP : DOWN;

        values.add(c.roundTickSize(key, base, mode));

        for (int i = 1; i < splits; i++) {

            BigDecimal previous = values.get(i - 1);

            BigDecimal adjusted = previous == null ? null : previous.add(deltaAmount);

            BigDecimal rounded = c.roundTickSize(key, adjusted, mode);

            values.add(rounded == null ? previous : rounded);

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

        instructions.addAll(remainingCreates.stream().filter(this::isValidInstruction).collect(toList()));

        instructions.forEach(v -> log.trace("Merged candidate : {}", v));

        return instructions;

    }

    private boolean isDifferent(BigDecimal v1, BigDecimal v2) {
        return v1 == null || v2 == null || v1.compareTo(v2) != 0;
    }

    private boolean isValidInstruction(CreateInstruction i) {
        return i != null && i.getPrice() != null && i.getSize() != null && i.getSize().signum() != 0;
    }

}
