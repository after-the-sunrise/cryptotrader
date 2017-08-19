package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.Bitflyer4j;
import com.after_sunrise.cryptocurrency.bitflyer4j.core.ProductType;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.*;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.AccountService;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.MarketService;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.OrderService;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.after_sunrise.cryptocurrency.bitflyer4j.core.ConditionType.LIMIT;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.ConditionType.MARKET;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType.BUY;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType.SELL;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.StateType.ACTIVE;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class BitflyerContext extends TemplateContext implements BitflyerService {

    private static final Duration CACHE = Duration.ofSeconds(3);

    private static final Duration TIMEOUT = Duration.ofMinutes(1);

    private Bitflyer4j bitflyer4j;

    private AccountService accountService;

    private MarketService marketService;

    private OrderService orderService;

    public BitflyerContext() {
        super(ID, CACHE);
    }

    @Inject
    public void initialize(Injector injector) {

        bitflyer4j = injector.getInstance(Bitflyer4j.class);

        accountService = bitflyer4j.getAccountService();

        marketService = bitflyer4j.getMarketService();

        orderService = bitflyer4j.getOrderService();

        log.debug("Initialized.");

    }

    @Override
    public BigDecimal getBestAskPrice(Key key) {

        Tick tick = findCached(Tick.class, key, () ->
                marketService.getTick(key.getInstrument()).get(TIMEOUT.toMillis(), MILLISECONDS)
        );

        return tick == null ? null : tick.getBestAskPrice();

    }

    @Override
    public BigDecimal getBestBidPrice(Key key) {

        Tick tick = findCached(Tick.class, key, () ->
                marketService.getTick(key.getInstrument()).get(TIMEOUT.toMillis(), MILLISECONDS)
        );

        return tick == null ? null : tick.getBestBidPrice();

    }

    @Override
    public BigDecimal getMidPrice(Key key) {

        Board val = findCached(Board.class, key, () ->
                marketService.getBoard(key.getInstrument()).get(TIMEOUT.toMillis(), MILLISECONDS)
        );

        return val == null ? null : val.getMid();

    }

    @Override
    public BigDecimal getLastPrice(Key key) {

        Tick tick = findCached(Tick.class, key, () ->
                marketService.getTick(key.getInstrument()).get(TIMEOUT.toMillis(), MILLISECONDS)
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

        List<Balance> balances = listCached(Balance.class, key, () ->
                accountService.getBalances().get(TIMEOUT.toMillis(), MILLISECONDS)
        );

        return Optional.ofNullable(balances).orElse(emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(b -> StringUtils.equals(currency, b.getCurrency()))
                .map(function)
                .findFirst().orElse(null);

    }

    @Override
    public BigDecimal getInstrumentPosition(Key key) {

        // TODO : Handle margin products (FX + Futures)

        return forBalance(key, ProductType::getStructure, Balance::getAmount);

    }

    @Override
    public BigDecimal getFundingPosition(Key key) {

        // TODO : Handle margin products (FX + Futures)

        return forBalance(key, ProductType::getFunding, Balance::getAmount);

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

        BitflyerOrder value = findCached(BitflyerOrder.class, key, () -> {

            OrderList r = OrderList.builder().acceptanceId(id).product(key.getInstrument()).build();

            Pagination p = Pagination.builder().count(NumberUtils.LONG_ONE).build();

            List<OrderList.Response> v = orderService.listOrders(r, p).get(TIMEOUT.toMillis(), MILLISECONDS);

            return Optional.ofNullable(v).orElse(emptyList()).stream()
                    .filter(Objects::nonNull).map(BitflyerOrder::new).findFirst().orElse(null);

        });

        return value;

    }

    @Override
    public List<Order> listOrders(Key key) {

        List<OrderList.Response> values = listCached(OrderList.Response.class, key, () -> {

            OrderList request = OrderList.builder().product(key.getInstrument()).state(ACTIVE).build();

            return orderService.listOrders(request, null).get(TIMEOUT.toMillis(), MILLISECONDS);

        });

        return values.stream().filter(Objects::nonNull).map(BitflyerOrder::new).collect(Collectors.toList());

    }

    @Override
    public String createOrder(Key key, Instruction.CreateInstruction instruction) {

        if (key == null || StringUtils.isBlank(key.getInstrument())) {
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

        CompletableFuture<OrderCreate.Response> future = orderService.sendOrder(create);

        return getQuietly(future.thenApply(r -> r == null ? null : r.getAcceptanceId()), TIMEOUT);

    }

    @Override
    public String cancelOrder(Key key, Instruction.CancelInstruction instruction) {

        if (key == null || StringUtils.isBlank(key.getInstrument())) {
            return null;
        }

        if (instruction == null || StringUtils.isEmpty(instruction.getId())) {
            return null;
        }

        OrderCancel.OrderCancelBuilder builder = OrderCancel.builder();
        builder.product(key.getInstrument());
        builder.acceptanceId(instruction.getId());
        OrderCancel cancel = builder.build();

        CompletableFuture<OrderCancel.Response> future = orderService.cancelOrder(cancel);

        return getQuietly(future.thenApply(r -> instruction.getId()), TIMEOUT);

    }

}
