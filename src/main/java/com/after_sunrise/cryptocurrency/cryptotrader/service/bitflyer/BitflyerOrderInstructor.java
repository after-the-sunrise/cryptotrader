package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.Bitflyer4j;
import com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.OrderList;
import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.OrderInstructor;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.PortfolioAdviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.impl.Frameworks;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;

import static com.after_sunrise.cryptocurrency.bitflyer4j.core.StateType.ACTIVE;
import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static java.math.RoundingMode.DOWN;
import static java.math.RoundingMode.UP;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class BitflyerOrderInstructor implements OrderInstructor {

    private static final int SCALE = 12;

    private static final BigDecimal INCREMENT = ONE.movePointLeft(SCALE);

    private PropertyManager propertyManager;

    private Bitflyer4j bitflyer4j;

    @Inject
    public void initialize(Injector injector) {

        propertyManager = injector.getInstance(PropertyManager.class);

        bitflyer4j = injector.getInstance(Bitflyer4j.class);

        log.debug("Initialized.");

    }

    @Override
    public String get() {
        return BitflyerService.ID;
    }

    @Override
    public List<Instruction> instruct(Context context, Request request, Advice advice) {

        if (Frameworks.isInvalid(request)) {

            log.trace("Invalid request : {}", request);

            return Collections.emptyList();

        }

        if (advice == null) {

            log.trace("Invalid advice : {}", request);

            return Collections.emptyList();

        }

        Key key = Frameworks.convert(request);

        Map<CancelInstruction, OrderList.Response> cancels = createCancels(context, key);

        List<CreateInstruction> creates = new ArrayList<>();
        creates.addAll(createBuys(context, key, advice));
        creates.addAll(createSells(context, key, advice));
        return merge(creates, cancels);

    }

    @VisibleForTesting
    Map<CancelInstruction, OrderList.Response> createCancels(Context context, Key key) {

        List<OrderList.Response> orders = Frameworks.trimToEmpty(context.getOrders(key, ACTIVE));

        Map<CancelInstruction, OrderList.Response> cancels = new IdentityHashMap<>();

        orders.forEach(o -> cancels.put(CancelInstruction.builder().id(o.getOrderId()).build(), o));

        log.trace("Cancel candidates : {}", cancels.keySet());

        return cancels;

    }

    @VisibleForTesting
    List<CreateInstruction> createBuys(Context context, Key key, Advice adv) {

        if (adv.getBuyLimitPrice() == null || adv.getBuyLimitPrice().signum() == 0) {
            return Collections.emptyList();
        }

        if (adv.getBuyLimitSize() == null || adv.getBuyLimitSize().signum() <= 0) {
            return Collections.emptyList();
        }

        List<BigDecimal> s = splitInstrumentSize(context, key, adv.getBuyLimitSize());

        List<BigDecimal> p = splitInstrumentPrice(context, key, adv.getBuyLimitPrice(), s.size(), INCREMENT.negate());

        List<CreateInstruction> instructions = new ArrayList<>(s.size());

        for (int i = 0; i < s.size(); i++) {
            instructions.add(CreateInstruction.builder().price(p.get(i)).size(s.get(i)).build());
        }

        log.trace("Buy candidates : {}", instructions);

        return instructions;

    }

    @VisibleForTesting
    List<CreateInstruction> createSells(Context context, Key key, Advice adv) {

        if (adv.getSellLimitPrice() == null || adv.getSellLimitPrice().signum() == 0) {
            return Collections.emptyList();
        }

        if (adv.getSellLimitSize() == null || adv.getSellLimitSize().signum() <= 0) {
            return Collections.emptyList();
        }

        List<BigDecimal> s = splitInstrumentSize(context, key, adv.getSellLimitSize());

        List<BigDecimal> p = splitInstrumentPrice(context, key, adv.getSellLimitPrice(), s.size(), INCREMENT);

        List<CreateInstruction> instructions = new ArrayList<>(s.size());

        for (int i = 0; i < s.size(); i++) {

            BigDecimal size = s.get(i) == null ? null : s.get(i).negate();

            instructions.add(CreateInstruction.builder().price(p.get(i)).size(size).build());

        }

        log.trace("Sell candidates : {}", instructions);

        return instructions;

    }

    @VisibleForTesting
    List<BigDecimal> splitInstrumentSize(Context context, Key key, BigDecimal value) {

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

        BigDecimal scaledBase = base.setScale(INTEGER_ZERO, DOWN).scaleByPowerOfTen(offset);

        BigDecimal split = propertyManager.getTradingSplit().setScale(INTEGER_ZERO, DOWN);

        BigDecimal slice = scaledBase.divide(split, SCALE, DOWN);

        BigDecimal rounded = context.roundInstrumentPosition(key, slice, DOWN);

        if (rounded == null || rounded.signum() <= 0) {
            return singletonList(value);
        }

        return Collections.nCopies(split.intValue(), rounded);

    }

    List<BigDecimal> splitInstrumentPrice(Context context, Key key, BigDecimal value, int size, BigDecimal delta) {

        List<BigDecimal> values = new ArrayList<>(size);

        values.add(value);

        for (int i = 1; i < size; i++) {

            BigDecimal previous = values.get(i - 1);

            BigDecimal raw = previous == null ? null : previous.add(delta);

            BigDecimal rounded = context.roundInstrumentPosition(key, raw, delta.signum() >= 0 ? UP : DOWN);

            values.add(rounded == null ? previous : rounded);

        }

        return values;

    }

    @VisibleForTesting
    List<Instruction> merge(List<CreateInstruction> creates, Map<CancelInstruction, OrderList.Response> cancels) {

        Map<CancelInstruction, OrderList.Response> remainingCancels = new IdentityHashMap<>(cancels);

        List<CreateInstruction> remainingCreates = new ArrayList<>(creates);

        Iterator<CreateInstruction> createItr = remainingCreates.iterator();

        while (createItr.hasNext()) {

            CreateInstruction create = createItr.next();

            Iterator<Entry<CancelInstruction, OrderList.Response>> cancelItr = remainingCancels.entrySet().iterator();

            while (cancelItr.hasNext()) {

                Entry<CancelInstruction, OrderList.Response> entry = cancelItr.next();

                if (entry.getValue().getPrice() == null) {
                    continue;
                }

                if (entry.getValue().getOutstandingSize() == null) {
                    continue;
                }

                if (create.getPrice().compareTo(entry.getValue().getPrice()) != 0) {
                    continue;
                }

                if (entry.getValue().getSide() == SideType.BUY) {
                    if (entry.getValue().getOutstandingSize().compareTo(create.getSize()) != 0) {
                        continue;
                    }
                } else {
                    if (entry.getValue().getOutstandingSize().negate().compareTo(create.getSize()) != 0) {
                        continue;
                    }
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
