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

import java.util.*;
import java.util.Map.Entry;

import static com.after_sunrise.cryptocurrency.bitflyer4j.core.StateType.ACTIVE;
import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class BitflyerOrderInstructor implements OrderInstructor {

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
    List<CreateInstruction> createBuys(Context context, Key key, Advice advice) {

        if (advice.getBuyLimitPrice() == null || advice.getBuyLimitPrice().signum() == 0) {
            return Collections.emptyList();
        }

        if (advice.getBuyLimitSize() == null || advice.getBuyLimitSize().signum() <= 0) {
            return Collections.emptyList();
        }

        CreateInstruction instruction = CreateInstruction.builder() //
                .price(advice.getBuyLimitPrice()) //
                .size(advice.getBuyLimitSize()).build();

        log.trace("Buy candidate : {}", instruction);

        return Collections.singletonList(instruction);

    }

    @VisibleForTesting
    List<CreateInstruction> createSells(Context context, Key key, Advice advice) {

        if (advice.getSellLimitPrice() == null || advice.getSellLimitPrice().signum() == 0) {
            return Collections.emptyList();
        }

        if (advice.getSellLimitSize() == null || advice.getSellLimitSize().signum() <= 0) {
            return Collections.emptyList();
        }

        CreateInstruction instruction = CreateInstruction.builder() //
                .price(advice.getSellLimitPrice()) //
                .size(advice.getSellLimitSize().negate()).build();

        log.trace("Sell candidate : {}", instruction);

        return Collections.singletonList(instruction);

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
