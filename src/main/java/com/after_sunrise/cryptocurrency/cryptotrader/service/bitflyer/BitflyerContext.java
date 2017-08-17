package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.Bitflyer4j;
import com.after_sunrise.cryptocurrency.bitflyer4j.core.ProductType;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.after_sunrise.cryptocurrency.bitflyer4j.core.ConditionType.LIMIT;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.ConditionType.MARKET;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType.BUY;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType.SELL;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ONE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class BitflyerContext extends TemplateContext implements BitflyerService {

    private static final Duration EXPIRY = Duration.ofSeconds(INTEGER_ONE);

    private static final long TIMEOUT = MINUTES.toMillis(INTEGER_ONE);

    private Bitflyer4j bitflyer4j;

    public BitflyerContext() {
        super(ID);
    }

    @Inject
    public void initialize(Injector injector) {

        bitflyer4j = injector.getInstance(Bitflyer4j.class);

        log.debug("Initialized.");

    }

    @Override
    public BigDecimal getBesAskPrice(Key key) {

        Tick tick = findCached(Tick.class, key, EXPIRY, () ->
                bitflyer4j.getMarketService().getTick(key.getInstrument()).get(TIMEOUT, MILLISECONDS)
        );

        return tick == null ? null : tick.getBestAskPrice();

    }

    @Override
    public BigDecimal getBesBidPrice(Key key) {

        Tick tick = findCached(Tick.class, key, EXPIRY, () ->
                bitflyer4j.getMarketService().getTick(key.getInstrument()).get(TIMEOUT, MILLISECONDS)
        );

        return tick == null ? null : tick.getBestBidPrice();

    }

    @Override
    public BigDecimal getMidPrice(Key key) {

        Board val = findCached(Board.class, key, EXPIRY, () ->
                bitflyer4j.getMarketService().getBoard(key.getInstrument()).get(TIMEOUT, MILLISECONDS)
        );

        return val == null ? null : val.getMid();

    }

    @Override
    public BigDecimal getLastPrice(Key key) {

        Tick tick = findCached(Tick.class, key, EXPIRY, () ->
                bitflyer4j.getMarketService().getTick(key.getInstrument()).get(TIMEOUT, MILLISECONDS)
        );

        return tick == null ? null : tick.getTradePrice();

    }

    @VisibleForTesting
    BigDecimal forBalance(Key key, Function<ProductType, ProductType> mapper, Function<Balance, BigDecimal> function) {

        if (key == null) {
            return null;
        }

        ProductType instrumentType = ProductType.find(key.getInstrument());

        if (instrumentType == null) {
            return null;
        }

        String currency = mapper.apply(instrumentType).name();

        List<Balance> balances = listCached(Balance.class, key, EXPIRY, () ->
                bitflyer4j.getAccountService().getBalances().get(TIMEOUT, MILLISECONDS)
        );

        for (Balance balance : balances) {
            if (StringUtils.equals(currency, balance.getCurrency())) {
                return function.apply(balance);
            }
        }

        return null;

    }

    @Override
    public BigDecimal getInstrumentPosition(Key key) {

        // TODO : Handle margin products (FX + Futures)

        return forBalance(key, ProductType::getStructure, Balance::getAvailable);

    }

    @Override
    public BigDecimal getFundingPosition(Key key) {

        // TODO : Handle margin products (FX + Futures)

        return forBalance(key, ProductType::getFunding, Balance::getAvailable);

    }

    @Override
    public BigDecimal roundLotSize(Key key, BigDecimal value, RoundingMode mode) {

        if (key == null || value == null || mode == null) {
            return null;
        }

        ProductType type = ProductType.find(key.getInstrument());

        if (type == null) {
            return null;
        }

        return type.roundToLotSize(value, mode);

    }

    @Override
    public BigDecimal roundTickSize(Key key, BigDecimal value, RoundingMode mode) {

        if (key == null || value == null || mode == null) {
            return null;
        }

        ProductType instrument = ProductType.find(key.getInstrument());

        if (instrument == null) {
            return null;
        }

        return instrument.roundToTickSize(value, mode);

    }

    @Override
    public Order findOrder(Key key, String id) {

        return listOrders(key).stream()
                .filter(o -> StringUtils.equals(o.getId(), id))
                .findFirst()
                .orElse(null);

    }

    @Override
    public List<Order> listOrders(Key key) {

        List<OrderList.Response> values = listCached(OrderList.Response.class, key, EXPIRY, () -> {

            OrderList request = OrderList.builder().product(key.getInstrument()).build();

            return bitflyer4j.getOrderService().listOrders(request, null).get(TIMEOUT, MILLISECONDS);

        });

        return values.stream().filter(Objects::nonNull).map(BitflyerOrder::new).collect(Collectors.toList());

    }

    @Override
    public String createOrder(Key key, Instruction.CreateInstruction instruction) {

        if (key == null) {
            return null;
        }

        if (instruction == null || instruction.getPrice() == null || instruction.getSize() == null) {
            return null;
        }

        OrderCreate.OrderCreateBuilder builder = OrderCreate.builder();
        builder.product(key.getInstrument());
        builder.type(instruction.getPrice().signum() == 0 ? MARKET : LIMIT);
        builder.side(instruction.getSize().signum() > 0 ? BUY : SELL);
        builder.price(instruction.getPrice());
        builder.size(instruction.getSize().abs());
        OrderCreate create = builder.build();

        CompletableFuture<OrderCreate.Response> future = bitflyer4j.getOrderService().sendOrder(create);

        CompletableFuture<String> f = future.thenApply(r -> r == null ? null : r.getAcceptanceId());

        try {
            return f.get(TIMEOUT, MILLISECONDS);
        } catch (Exception e) {
            return null;
        }

    }

    @Override
    public String cancelOrder(Key key, Instruction.CancelInstruction instruction) {

        if (key == null) {
            return null;
        }

        if (instruction == null || StringUtils.isEmpty(instruction.getId())) {
            return null;
        }

        OrderCancel.OrderCancelBuilder builder = OrderCancel.builder();
        builder.product(key.getInstrument());
        builder.acceptanceId(instruction.getId());
        OrderCancel cancel = builder.build();

        CompletableFuture<OrderCancel.Response> future = bitflyer4j.getOrderService().cancelOrder(cancel);

        CompletableFuture<String> f = future.thenApply(r -> instruction.getId());

        try {
            return f.get(TIMEOUT, MILLISECONDS);
        } catch (Exception e) {
            return null;
        }

    }

}
