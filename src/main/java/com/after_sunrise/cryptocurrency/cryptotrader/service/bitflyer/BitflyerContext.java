package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.Bitflyer4j;
import com.after_sunrise.cryptocurrency.bitflyer4j.Bitflyer4jFactory;
import com.after_sunrise.cryptocurrency.bitflyer4j.core.ConditionType;
import com.after_sunrise.cryptocurrency.bitflyer4j.core.ParentType;
import com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.*;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.CollectionUtils;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.after_sunrise.cryptocurrency.bitflyer4j.core.ConditionType.*;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.ParentType.IFD;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType.BUY;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType.SELL;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.AssetType.COLLATERAL;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ProductType.COLLATERAL_BTC;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ProductType.COLLATERAL_JPY;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.math.RoundingMode.UP;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.math.NumberUtils.LONG_ONE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitflyerContext extends TemplateContext implements BitflyerService, RealtimeListener {

    private static final Pattern EXPIRY_PATTERN = Pattern.compile("^[A-Z]{6}[0-9]{2}[A-Z]{3}[0-9]{4}$");

    private static final String EXPIRY_TIME = "1100";

    private static final ThreadLocal<DateFormat> EXPIRY_FORMAT = ThreadLocal.withInitial(() -> {
        DateFormat df = new SimpleDateFormat("ddMMMyyyyHHmm", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        return df;
    });

    private static final Duration TIMEOUT = Duration.ofMinutes(3);

    private static final Duration REALTIME_EXPIRY = Duration.ofMinutes(1);

    private static final Duration REALTIME_TRADE = Duration.ofDays(3);

    private static final int REALTIME_COUNT = 1000;

    private static final int REALTIME_QUERIES = 64;

    private final Bitflyer4j bitflyer4j;

    private final AccountService accountService;

    private final MarketService marketService;

    private final OrderService orderService;

    private final RealtimeService realtimeService;

    private final Map<String, Lock> realtimeLocks;

    private final Map<String, Optional<BitflyerBoard>> realtimeBoards;

    private final Map<String, Optional<Tick>> realtimeTicks;

    private final Map<String, NavigableMap<Instant, BitflyerTrade>> realtimeTrades;

    public BitflyerContext() {

        this(new Bitflyer4jFactory().createInstance());

    }

    @VisibleForTesting
    BitflyerContext(Bitflyer4j api) {

        super(ID);

        realtimeLocks = synchronizedMap(new HashMap<>());

        realtimeBoards = new ConcurrentHashMap<>();

        realtimeTicks = new ConcurrentHashMap<>();

        realtimeTrades = new ConcurrentHashMap<>();

        bitflyer4j = api;

        accountService = bitflyer4j.getAccountService();

        marketService = bitflyer4j.getMarketService();

        orderService = bitflyer4j.getOrderService();

        realtimeService = bitflyer4j.getRealtimeService();

        realtimeService.addListener(this);

        log.debug("Initialized.");

    }

    @Override
    public void close() throws Exception {

        bitflyer4j.close();

    }

    @Override
    public void onBoards(String product, Board value) {
        // TODO
    }

    @Override
    public void onBoardsSnapshot(String product, Board value) {

        if (value == null) {
            return;
        }

        String key = StringUtils.trimToEmpty(product);

        Instant timestamp = getNow();

        realtimeBoards.put(key, Optional.of(new BitflyerBoard(timestamp, value)));

    }

    @Override
    public void onTicks(String product, List<Tick> values) {

        if (CollectionUtils.isEmpty(values)) {
            return;
        }

        values.stream().filter(Objects::nonNull).forEach(t -> {

            String key = StringUtils.trimToEmpty(product);

            realtimeTicks.put(key, Optional.of(t));

        });

    }

    @Override
    public void onExecutions(String product, List<Execution> values) {

        if (CollectionUtils.isEmpty(values)) {
            return;
        }

        String id = StringUtils.trimToEmpty(product);

        NavigableMap<Instant, BitflyerTrade> trades = realtimeTrades.get(id);

        if (trades == null) {
            return;
        }

        updateExecutions(trades, values);

    }

    @VisibleForTesting
    void updateExecutions(NavigableMap<Instant, BitflyerTrade> trades, List<Execution> values) {

        if (trades == null || values == null) {
            return;
        }

        values.stream().filter(Objects::nonNull)
                .filter(exec -> exec.getTimestamp() != null)
                .filter(exec -> exec.getPrice() != null)
                .filter(exec -> exec.getPrice().signum() != 0)
                .filter(exec -> exec.getSize() != null)
                .filter(exec -> exec.getSize().signum() != 0)
                .forEach(exec -> {

                    Instant time = exec.getTimestamp().plus(LONG_ONE, SECONDS).truncatedTo(SECONDS).toInstant();

                    BitflyerTrade trade = trades.get(time);

                    if (trade != null) {

                        trade.accumulate(exec.getPrice(), exec.getSize());

                        return;

                    }

                    trades.put(time, new BitflyerTrade(time, exec.getPrice(), exec.getSize()));

                });

        Instant cutoff = getNow().minus(REALTIME_TRADE);

        while (true) {

            Map.Entry<Instant, BitflyerTrade> entry = trades.firstEntry();

            if (entry == null) {
                break;
            }

            if (entry.getKey().isAfter(cutoff)) {
                break;
            }

            trades.remove(entry.getKey());

        }

    }

    @VisibleForTesting
    String convertProductAlias(Key key) {

        if (key == null || StringUtils.isEmpty(key.getInstrument())) {
            return null;
        }

        Key all = Key.build(key).instrument(WILDCARD).build();

        List<Product> products = listCached(Product.class, all, () ->
                extract(marketService.getProducts(), TIMEOUT)
        );

        return trimToEmpty(products).stream()
                .filter(Objects::nonNull)
                .filter(p -> StringUtils.isNotEmpty(p.getProduct()))
                .filter(p ->
                        StringUtils.equals(key.getInstrument(), p.getProduct())
                                || StringUtils.equals(key.getInstrument(), p.getAlias())
                )
                .map(Product::getProduct)
                .findFirst().orElse(null);

    }

    @VisibleForTesting
    BitflyerBoard getBoard(Key key) {

        return findCached(BitflyerBoard.class, key, () -> {

            String instrument = StringUtils.trimToEmpty(convertProductAlias(key));

            Optional<BitflyerBoard> realtime = realtimeBoards.get(instrument);

            if (realtime == null) {

                // Initiate subscription if nothing is cached.

                realtime = Optional.empty();

                realtimeBoards.put(instrument, realtime);

                realtimeService.subscribeBoard(singletonList(instrument));

            }

            if (realtime
                    .filter(board -> board.getTimestamp() != null)
                    .map(board -> Duration.between(board.getTimestamp(), key.getTimestamp()))
                    .filter(duration -> duration.compareTo(REALTIME_EXPIRY) <= 0)
                    .isPresent()) {

                // Use cached if the timestamp is not old.

                return realtime.orElse(null);

            }

            // Fall back to request/response.

            Board.Request request = Board.Request.builder().product(instrument).build();

            Board board = extract(marketService.getBoard(request), TIMEOUT);

            return board == null ? null : new BitflyerBoard(getNow(), board);

        });

    }

    @VisibleForTesting
    Tick getTick(Key key) {

        return findCached(Tick.class, key, () -> {

            String instrument = StringUtils.trimToEmpty(convertProductAlias(key));

            Optional<Tick> realtime = realtimeTicks.get(instrument);

            if (realtime == null) {

                // Initiate subscription if nothing is cached.

                realtime = Optional.empty();

                realtimeTicks.put(instrument, realtime);

                realtimeService.subscribeTick(singletonList(instrument));

            }

            if (realtime
                    .filter(tick -> tick.getTimestamp() != null)
                    .map(tick -> Duration.between(tick.getTimestamp().toInstant(), key.getTimestamp()))
                    .filter(duration -> duration.compareTo(REALTIME_EXPIRY) <= 0)
                    .isPresent()) {

                // Use cached if the timestamp is not old.

                return realtime.orElse(null);

            }

            // Fall back to request/response.

            Tick.Request request = Tick.Request.builder().product(instrument).build();

            return extract(marketService.getTick(request), TIMEOUT);

        });

    }

    @Override
    public BigDecimal getBestAskPrice(Key key) {

        Tick tick = getTick(key);

        return tick == null ? null : tick.getBestAskPrice();

    }

    @Override
    public BigDecimal getBestBidPrice(Key key) {

        Tick tick = getTick(key);

        return tick == null ? null : tick.getBestBidPrice();

    }

    @Override
    public BigDecimal getBestAskSize(Key key) {

        Tick tick = getTick(key);

        return tick == null ? null : tick.getBestAskSize();

    }

    @Override
    public BigDecimal getBestBidSize(Key key) {

        Tick tick = getTick(key);

        return tick == null ? null : tick.getBestBidSize();

    }

    @Override
    public BigDecimal getLastPrice(Key key) {

        Tick tick = getTick(key);

        return tick == null ? null : tick.getTradePrice();

    }

    @Override
    public List<Trade> listTrades(Key key, Instant fromTime) {

        String id = StringUtils.trimToEmpty(convertProductAlias(key));

        Lock lock = realtimeLocks.computeIfAbsent(id, k -> new ReentrantLock());

        NavigableMap<Instant, BitflyerTrade> trades;

        try {

            lock.lock();

            trades = realtimeTrades.get(id);

            if (trades == null) {

                realtimeService.subscribeExecution(singletonList(id));

                trades = new ConcurrentSkipListMap<>();

                Execution.Request.RequestBuilder b = Execution.Request.builder().product(id).count(REALTIME_COUNT);

                Instant cutoff = getNow().minus(REALTIME_TRADE);

                Long minimumId = null;

                for (int i = 0; i < REALTIME_QUERIES; i++) {

                    Execution.Request r = b.before(minimumId).build();

                    List<Execution> execs = trimToEmpty(extractQuietly(marketService.getExecutions(r), TIMEOUT));

                    updateExecutions(trades, execs);

                    minimumId = execs.stream().filter(Objects::nonNull)
                            .filter(e -> e.getId() != null)
                            .filter(e -> e.getTimestamp() != null)
                            .filter(e -> e.getTimestamp().toInstant().isAfter(cutoff))
                            .min(Comparator.comparing(Execution::getId))
                            .map(Execution::getId)
                            .orElse(null);

                    if (minimumId == null) {
                        break;
                    }

                }

                realtimeTrades.put(id, trades);

            }

        } finally {
            lock.unlock();
        }

        Instant cutoff = fromTime != null ? fromTime : getNow().minus(REALTIME_TRADE);

        return trades.values().stream()
                .filter(trade -> trade.getTimestamp() != null)
                .filter(trade -> trade.getTimestamp().isAfter(cutoff))
                .map(BitflyerTrade::snapshot)
                .collect(toList());

    }

    @VisibleForTesting
    CurrencyType getCurrency(Key key, Function<ProductType, CurrencyType> f) {

        if (key == null || f == null) {
            return null;
        }

        ProductType product = ProductType.find(key.getInstrument());

        return product == null ? null : f.apply(product);

    }

    @Override
    public CurrencyType getInstrumentCurrency(Key key) {

        return getCurrency(key, p -> p.getStructure().getCurrency());

    }

    @Override
    public CurrencyType getFundingCurrency(Key key) {

        return getCurrency(key, p -> p.getFunding().getCurrency());

    }

    @Override
    public BigDecimal getConversionPrice(Key key, CurrencyType currency) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        CurrencyType structureCurrency = product.getStructure().getCurrency();

        if (structureCurrency == currency) {
            return ONE;
        }

        for (ProductType p : ProductType.values()) {

            if (p.getFunding().getCurrency() != structureCurrency) {
                continue;
            }

            if (p.getStructure().getCurrency() != currency) {
                continue;
            }

            BigDecimal price = getMidPrice(Key.build(key).instrument(p.name()).build());

            return price == null || price.signum() == 0 ? null : price;

        }

        for (ProductType p : ProductType.values()) {

            if (p.getStructure().getCurrency() != structureCurrency) {
                continue;
            }

            if (p.getFunding().getCurrency() != currency) {
                continue;
            }

            BigDecimal price = getMidPrice(Key.build(key).instrument(p.name()).build());

            return price == null || price.signum() == 0 ? null : ONE.divide(price, SCALE, HALF_UP);

        }

        return null;

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

        Key all = Key.build(key).instrument(WILDCARD).build();

        List<Balance> balances = listCached(Balance.class, all, () ->
                extract(accountService.getBalances(), TIMEOUT)
        );

        String currency = mapper.apply(product).name();

        return trimToEmpty(balances).stream()
                .filter(Objects::nonNull)
                .filter(b -> StringUtils.equals(currency, b.getCurrency()))
                .map(Balance::getAmount)
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

        AssetType asset = mapper.apply(product);

        if (asset == COLLATERAL) {

            Key all = Key.build(key).instrument(WILDCARD).build();

            Collateral collateral = findCached(Collateral.class, all, () ->
                    extract(accountService.getCollateral(), TIMEOUT)
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

        if (product == COLLATERAL_JPY || product == COLLATERAL_BTC) {

            Key all = Key.build(key).instrument(WILDCARD).build();

            List<Margin> margins = listCached(Margin.class, all, () ->
                    extract(accountService.getMargins(), TIMEOUT)
            );

            if (CollectionUtils.isEmpty(margins)) {
                return null;
            }

            return margins.stream()
                    .filter(Objects::nonNull)
                    .filter(c -> asset == AssetType.find(c.getCurrency()))
                    .findFirst()
                    .map(Margin::getAmount)
                    .orElse(null);

        }

        List<TradePosition> positions = listCached(TradePosition.class, key, () -> {

            // "BTCJPY_MAT2WK" -> "BTCJPYddmmmyyyy"
            String productId = convertProductAlias(key);

            TradePosition.Request request = TradePosition.Request.builder().product(productId).build();

            return extract(orderService.listPositions(request), TIMEOUT);

        });

        Optional<BigDecimal> sum = trimToEmpty(positions).stream()
                .filter(Objects::nonNull)
                .filter(p -> Objects.nonNull(p.getSide()))
                .filter(p -> Objects.nonNull(p.getSize()))
                .map(p -> p.getSide() == BUY ? p.getSize() : p.getSize().negate())
                .reduce(BigDecimal::add);

        return sum.orElse(ZERO);

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

        TradeCommission value = findCached(TradeCommission.class, key, () -> {

            TradeCommission.Request request = TradeCommission.Request.builder()
                    .product(key.getInstrument()).build();

            return extract(orderService.getCommission(request), TIMEOUT);

        });

        return value == null ? null : value.getRate();

    }

    @Override
    public Boolean isMarginable(Key key) {

        if (key == null) {
            return false;
        }

        ProductType type = ProductType.find(key.getInstrument());

        return type != null && type.getFunding() == COLLATERAL;

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
    List<BitflyerOrder> fetchOrder(Key key) {

        return listCached(BitflyerOrder.class, key, () -> {

            String product = convertProductAlias(key);

            List<BitflyerOrder> values = new ArrayList<>();

            trimToEmpty(extract(orderService.listOrders(
                    OrderList.Request.builder().product(product).build()), TIMEOUT)
            ).stream().filter(Objects::nonNull).map(BitflyerOrder.Child::new).forEach(values::add);

            trimToEmpty(extract(orderService.listParents(
                    ParentList.Request.builder().product(product).build()), TIMEOUT)
            ).stream().filter(Objects::nonNull).map(BitflyerOrder.Parent::new).forEach(values::add);

            return unmodifiableList(values);

        });

    }

    @Override
    public BitflyerOrder findOrder(Key key, String id) {

        return fetchOrder(key).stream().filter(o -> StringUtils.isNotEmpty(o.getId()))
                .filter(o -> StringUtils.equals(o.getId(), id)).findFirst().orElse(null);

    }

    @Override
    public List<Order> listActiveOrders(Key key) {

        return fetchOrder(key).stream().filter(o -> TRUE.equals(o.getActive())).collect(toList());

    }

    @Override
    public List<Order.Execution> listExecutions(Key key) {

        List<BitflyerExecution> execs = listCached(BitflyerExecution.class, key, () -> {

            String product = convertProductAlias(key);

            TradeExecution.Request request = TradeExecution.Request.builder().product(product).build();

            return unmodifiableList(trimToEmpty(extract(orderService.listExecutions(request), TIMEOUT))
                    .stream().filter(Objects::nonNull).map(BitflyerExecution::new).collect(toList()));

        });

        return new ArrayList<>(execs);

    }

    @Override
    public Map<CreateInstruction, String> createOrders(Key key, Set<CreateInstruction> instructions) {

        if (CollectionUtils.isEmpty(instructions)) {
            return emptyMap();
        }

        Map<CreateInstruction, CompletableFuture<String>> futures = new IdentityHashMap<>();

        for (CreateInstruction instruction : instructions) {

            if (key == null || StringUtils.isBlank(key.getInstrument())) {
                futures.put(instruction, CompletableFuture.completedFuture(null));
                continue;
            }

            if (instruction == null || instruction.getPrice() == null
                    || instruction.getSize() == null || instruction.getSize().signum() == 0) {
                futures.put(instruction, CompletableFuture.completedFuture(null));
                continue;
            }

            String product = convertProductAlias(key);
            ConditionType condition = instruction.getPrice().signum() == 0 ? MARKET : LIMIT;
            SideType side = instruction.getSize().signum() > 0 ? BUY : SELL;
            BigDecimal size = instruction.getSize().abs();
            BigDecimal price = instruction.getPrice();

            if (IFD.name().equals(instruction.getStrategy())) {

                BigDecimal lotSize = roundLotSize(key, EPSILON, UP);

                if (lotSize != null && lotSize.compareTo(size) < 0) {

                    ParentCreate.Request.Parameter.ParameterBuilder pb = ParentCreate.Request.Parameter.builder()
                            .product(product).condition(condition).side(side).price(price);

                    CompletableFuture<ParentCreate> future = orderService.sendParent(ParentCreate.Request.builder()
                            .type(IFD)
                            .parameters(Arrays.asList(
                                    pb.size(lotSize).build(),
                                    pb.size(size.subtract(lotSize)).build()
                            )).build());

                    futures.put(instruction, future.thenApply(r -> r == null ? null : r.getAcceptanceId()));

                    continue;

                }

            }

            if (STOP.name().equals(instruction.getStrategy())) {

                CompletableFuture<ParentCreate> future = orderService.sendParent(ParentCreate.Request.builder()
                        .type(ParentType.SIMPLE)
                        .parameters(singletonList(ParentCreate.Request.Parameter.builder()
                                .product(product).condition(STOP).side(side == BUY ? SELL : BUY)
                                .triggerPrice(price).size(size).build()
                        )).build());

                futures.put(instruction, future.thenApply(r -> r == null ? null : r.getAcceptanceId()));

                continue;

            }

            if (STOP_LIMIT.name().equals(instruction.getStrategy())) {

                CompletableFuture<ParentCreate> future = orderService.sendParent(ParentCreate.Request.builder()
                        .type(ParentType.SIMPLE)
                        .parameters(singletonList(ParentCreate.Request.Parameter.builder()
                                .product(product).condition(STOP_LIMIT).side(side == BUY ? SELL : BUY)
                                .price(price).triggerPrice(price).size(size).build()
                        )).build());

                futures.put(instruction, future.thenApply(r -> r == null ? null : r.getAcceptanceId()));

                continue;

            }

            CompletableFuture<OrderCreate> future = orderService.sendOrder(OrderCreate.Request.builder()
                    .product(product).type(condition).side(side).price(price).size(size).build()
            );

            futures.put(instruction, future.thenApply(r -> r == null ? null : r.getAcceptanceId()));

        }

        Map<CreateInstruction, String> results = new IdentityHashMap<>();

        futures.forEach((k, v) -> results.put(k, extractQuietly(v, TIMEOUT)));

        return results;

    }

    @Override
    public Map<CancelInstruction, String> cancelOrders(Key key, Set<CancelInstruction> instructions) {

        if (CollectionUtils.isEmpty(instructions)) {
            return emptyMap();
        }

        Map<CancelInstruction, CompletableFuture<String>> futures = new IdentityHashMap<>();

        for (CancelInstruction instruction : instructions) {

            if (key == null || StringUtils.isBlank(key.getInstrument())) {

                futures.put(instruction, CompletableFuture.completedFuture(null));

                continue;

            }

            if (instruction == null || StringUtils.isEmpty(instruction.getId())) {

                futures.put(instruction, CompletableFuture.completedFuture(null));

                continue;

            }

            BitflyerOrder order = findOrder(key, instruction.getId());

            if (order == null) {

                futures.put(instruction, CompletableFuture.completedFuture(null));

                continue;

            }

            CompletableFuture<String> future = order.accept(new BitflyerOrder.Visitor<CompletableFuture<String>>() {
                @Override
                public CompletableFuture<String> visit(BitflyerOrder.Child order) {

                    OrderCancel.Request.RequestBuilder builder = OrderCancel.Request.builder();
                    builder.product(convertProductAlias(key));
                    builder.acceptanceId(instruction.getId());

                    CompletableFuture<OrderCancel> f = orderService.cancelOrder(builder.build());

                    return f.thenApply(r -> r == null ? null : instruction.getId());

                }

                @Override
                public CompletableFuture<String> visit(BitflyerOrder.Parent order) {

                    ParentCancel.Request.RequestBuilder builder = ParentCancel.Request.builder();
                    builder.product(convertProductAlias(key));
                    builder.acceptanceId(instruction.getId());

                    CompletableFuture<ParentCancel> f = orderService.cancelParent(builder.build());

                    return f.thenApply(r -> r == null ? null : instruction.getId());

                }
            });

            futures.put(instruction, future);

        }

        Map<CancelInstruction, String> results = new IdentityHashMap<>();

        futures.forEach((k, v) -> results.put(k, extractQuietly(v, TIMEOUT)));

        return results;

    }

}
