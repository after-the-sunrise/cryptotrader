package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.Bitflyer4j;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.OrderCancel;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.OrderCreate;
import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.OrderManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.impl.Frameworks;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.CompletableFuture;

import static com.after_sunrise.cryptocurrency.bitflyer4j.core.ConditionType.LIMIT;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.ConditionType.MARKET;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType.BUY;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType.SELL;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class BitflyerOrderManager implements OrderManager {

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
    public Boolean create(Request request, CreateInstruction instruction) {

        if (Frameworks.isInvalid(request)) {

            log.trace("Invalid request : {}", request);

            return Boolean.FALSE;

        }

        if (instruction == null || instruction.getPrice() == null || instruction.getSize() == null) {

            log.trace("Invalid instruction : {}", instruction);

            return Boolean.FALSE;

        }

        OrderCreate.OrderCreateBuilder builder = OrderCreate.builder();
        builder.product(request.getInstrument());
        builder.type(instruction.getPrice().signum() == 0 ? MARKET : LIMIT);
        builder.side(instruction.getSize().signum() > 0 ? BUY : SELL);
        builder.price(instruction.getPrice());
        builder.size(instruction.getSize());
        OrderCreate create = builder.build();

        if (!propertyManager.getTradingActive()) {

            log.debug("Skipping due to dry-run : {}", create);

            return Boolean.TRUE;

        }

        CompletableFuture<OrderCreate.Response> future = bitflyer4j.getOrderService().sendOrder(create);

        try {

            OrderCreate.Response response = future.get();

            log.debug("Created order : {} - {}", response, create);

            return Boolean.TRUE;

        } catch (Exception e) {

            log.warn("Failed to create order : " + create, e);

            return Boolean.FALSE;

        }

    }

    @Override
    public Boolean cancel(Request request, CancelInstruction instruction) {

        if (Frameworks.isInvalid(request)) {

            log.trace("Invalid request : {}", request);

            return Boolean.FALSE;

        }

        if (instruction == null || StringUtils.isEmpty(instruction.getId())) {

            log.trace("Invalid instruction : {}", instruction);

            return Boolean.FALSE;

        }

        OrderCancel.OrderCancelBuilder builder = OrderCancel.builder();
        builder.product(request.getInstrument());
        builder.orderId(instruction.getId());
        OrderCancel cancel = builder.build();

        if (!propertyManager.getTradingActive()) {

            log.debug("Skipping due to dry-run : {}", cancel);

            return Boolean.TRUE;

        }

        CompletableFuture<OrderCancel.Response> future = bitflyer4j.getOrderService().cancelOrder(cancel);

        try {

            OrderCancel.Response response = future.get();

            log.debug("Cancelled order : {} - {}", response, cancel);

            return Boolean.TRUE;

        } catch (Exception e) {

            log.warn("Failed to cancel order : " + cancel, e);

            return Boolean.FALSE;

        }

    }

}
