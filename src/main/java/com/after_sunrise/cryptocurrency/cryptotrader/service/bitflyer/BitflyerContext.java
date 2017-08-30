package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.Bitflyer4j;
import com.after_sunrise.cryptocurrency.bitflyer4j.Bitflyer4jFactory;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.*;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.AccountService;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.MarketService;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.OrderService;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.after_sunrise.cryptocurrency.bitflyer4j.core.ConditionType.LIMIT;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.ConditionType.MARKET;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType.BUY;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType.SELL;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.AssetType.COLLATERAL;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class BitflyerContext extends TemplateContext implements BitflyerService {

    private static final Pattern EXPIRY_PATTERN = Pattern.compile("^[A-Z]{6}[0-9]{2}[A-Z]{3}[0-9]{4}$");

    private static final String EXPIRY_TIME = "1600";

    private static final ThreadLocal<DateFormat> EXPIRY_FORMAT = ThreadLocal.withInitial(() -> {
        DateFormat df = new SimpleDateFormat("ddMMMyyyyHHmm", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        return df;
    });

    private static final Duration TIMEOUT = Duration.ofMinutes(1);

    private static final BigDecimal HALF = new BigDecimal("0.5");

    private static final Long PAGE = 500L;

    private Bitflyer4j bitflyer4j;

    private AccountService accountService;

    private MarketService marketService;

    private OrderService orderService;

    public BitflyerContext() {

        this(new Bitflyer4jFactory().createInstance());

    }

    @VisibleForTesting
    BitflyerContext(Bitflyer4j api) {

        super(ID);

        bitflyer4j = api;

        accountService = bitflyer4j.getAccountService();

        marketService = bitflyer4j.getMarketService();

        orderService = bitflyer4j.getOrderService();

        log.debug("Initialized.");

    }

    @Override
    public void close() throws Exception {

        bitflyer4j.close();

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

        Tick tick = findCached(Tick.class, key, () ->
                marketService.getTick(key.getInstrument()).get(TIMEOUT.toMillis(), MILLISECONDS)
        );

        if (tick == null) {
            return null;
        }

        BigDecimal ask = tick.getBestAskPrice();

        BigDecimal bid = tick.getBestBidPrice();

        if (ask == null || bid == null) {
            return null;
        }

        return ask.add(bid).multiply(HALF);

    }

    @Override
    public BigDecimal getLastPrice(Key key) {

        Tick tick = findCached(Tick.class, key, () ->
                marketService.getTick(key.getInstrument()).get(TIMEOUT.toMillis(), MILLISECONDS)
        );

        return tick == null ? null : tick.getTradePrice();

    }

    @Override
    public List<Trade> listTrades(Key key, Instant fromTime) {

        List<Trade> values = listCached(Trade.class, key, () -> {

            String instrument = key.getInstrument();

            Pagination p = Pagination.builder().count(PAGE).build();

            return unmodifiableList(ofNullable(marketService.getExecutions(instrument, p)
                    .get(TIMEOUT.toMillis(), MILLISECONDS)).orElse(emptyList())
                    .stream().filter(Objects::nonNull).map(BitflyerTrade::new).collect(toList()));

        });

        if (fromTime == null) {
            return values;
        }

        return unmodifiableList(values.stream()
                .filter(e -> Objects.nonNull(e.getTimestamp()))
                .filter(e -> !e.getTimestamp().isBefore(fromTime))
                .collect(toList()));

    }

    @Override
    public BigDecimal getInstrumentPosition(Key key) {

        return forBalance(key, ProductType::getStructure);

    }

    @Override
    public BigDecimal getFundingPosition(Key key) {

        return forBalance(key, ProductType::getFunding);

    }

    @VisibleForTesting
    BigDecimal forBalance(Key key, Function<ProductType, AssetType> mapper) {

        if (key == null) {
            return null;
        }

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        if (product.getFunding() == COLLATERAL) {
            return forMargin(key, mapper);
        }

        String currency = mapper.apply(product).name();

        List<Balance> balances = listCached(Balance.class, key, () ->
                accountService.getBalances().get(TIMEOUT.toMillis(), MILLISECONDS)
        );

        return ofNullable(balances).orElse(emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(b -> StringUtils.equals(currency, b.getCurrency()))
                .map(b -> b.getAmount())
                .findFirst().orElse(null);

    }

    @VisibleForTesting
    BigDecimal forMargin(Key key, Function<ProductType, AssetType> mapper) {

        if (key == null) {
            return null;
        }

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null || product.getFunding() != COLLATERAL) {
            return null;
        }

        if (mapper.apply(product) == COLLATERAL) {

            Collateral collateral = findCached(Collateral.class, key, () ->
                    accountService.getCollateral().get(TIMEOUT.toMillis(), MILLISECONDS)
            );

            if (collateral == null) {
                return null;
            }

            BigDecimal amount = collateral.getCollateral();

            if (amount == null) {
                return null;
            }

            BigDecimal required = collateral.getRequiredCollateral();

            if (required == null) {
                return null;
            }

            return amount.subtract(required);

        }

        List<TradePosition.Response> positions = listCached(TradePosition.Response.class, key, () -> {

            // "BTCJPY_MAT2WK" -> "BTCJPYddmmmyyyy"
            String productId = convertProductAlias(key);

            TradePosition request = TradePosition.builder().product(productId).build();

            return orderService.listPositions(request).get(TIMEOUT.toMillis(), MILLISECONDS);

        });

        Optional<BigDecimal> sum = ofNullable(positions).orElse(emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(p -> Objects.nonNull(p.getSide()))
                .filter(p -> Objects.nonNull(p.getSize()))
                .map(p -> p.getSide() == BUY ? p.getSize() : p.getSize().negate())
                .reduce(BigDecimal::add);

        return sum.orElse(ZERO);

    }

    @VisibleForTesting
    String convertProductAlias(Key key) {

        if (key == null || StringUtils.isEmpty(key.getInstrument())) {
            return null;
        }

        List<Product> products = listCached(Product.class, key, () ->
                marketService.getProducts().get(TIMEOUT.toMillis(), MILLISECONDS)
        );

        return ofNullable(products).orElse(emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(p -> StringUtils.isNotEmpty(p.getProduct()))
                .filter(p ->
                        StringUtils.equals(key.getInstrument(), p.getProduct())
                                || StringUtils.equals(key.getInstrument(), p.getAlias())
                )
                .map(Product::getProduct)
                .findFirst().orElse(null);

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
    public BigDecimal getCommissionRate(Key key) {

        TradeCommission.Response value = findCached(TradeCommission.Response.class, key, () -> {

            TradeCommission request = TradeCommission.builder().product(key.getInstrument()).build();

            return orderService.getCommission(request).get(TIMEOUT.toMillis(), MILLISECONDS);

        });

        return value == null ? null : value.getRate();

    }

    @Override
    public Boolean isMarginable(Key key) {

        if (key == null) {
            return false;
        }

        ProductType type = ProductType.find(key.getInstrument());

        if (type == null) {
            return false;
        }

        return type.getFunding() == COLLATERAL;

    }

    @Override
    public ZonedDateTime getExpiry(Key key) {

        String code = StringUtils.trimToEmpty(convertProductAlias(key));

        if (!EXPIRY_PATTERN.matcher(code).matches()) {
            return null;
        }

        ZonedDateTime expiry;

        try {

            String value = code.substring(6) + EXPIRY_TIME;

            Date date = EXPIRY_FORMAT.get().parse(value);

            ZoneId zone = ZoneId.of(EXPIRY_FORMAT.get().getTimeZone().getID());

            expiry = ZonedDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), zone);

        } catch (ParseException e) {

            expiry = null;

        }

        return expiry;

    }

    @VisibleForTesting
    List<Order> fetchOrder(Key key) {

        List<Order> values = listCached(Order.class, key, () -> {

            OrderList request = OrderList.builder().product(key.getInstrument()).build();

            return unmodifiableList(ofNullable(orderService.listOrders(request, null)
                    .get(TIMEOUT.toMillis(), MILLISECONDS)).orElse(emptyList()).stream()
                    .filter(Objects::nonNull).map(BitflyerOrder::new).collect(toList()));

        });

        return values;

    }

    @Override
    public Order findOrder(Key key, String id) {

        return fetchOrder(key).stream().filter(o -> StringUtils.isNotEmpty(o.getId()))
                .filter(o -> StringUtils.equals(o.getId(), id)).findFirst().orElse(null);

    }

    @Override
    public List<Order> listActiveOrders(Key key) {

        return fetchOrder(key).stream().filter(o -> TRUE.equals(o.getActive())).collect(toList());

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
