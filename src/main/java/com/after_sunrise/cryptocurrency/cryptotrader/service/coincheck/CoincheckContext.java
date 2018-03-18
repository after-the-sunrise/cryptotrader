package com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.websocket.*;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck.CoincheckService.SideType.*;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType.*;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Collections.emptyList;
import static java.util.Collections.synchronizedMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@ClientEndpoint
public class CoincheckContext extends TemplateContext implements CoincheckService {

    private static final String URL_TICK = "https://coincheck.com/api/ticker";

    private static final String URL_BOOK = "https://coincheck.com/api/order_books";

    private static final String URL_TRADE = "https://coincheck.com/api/trades";

    private static final String URL_BALANCE = "https://coincheck.com/api/accounts/balance";

    private static final String URL_ORDER_LIST = "https://coincheck.com/api/exchange/orders/opens";

    private static final String URL_EXECUTION = "https://coincheck.com/api/exchange/orders/transactions";

    private static final String URL_ORDER_CREATE = "https://coincheck.com/api/exchange/orders";

    private static final String URL_ORDER_CANCEL = "https://coincheck.com/api/exchange/orders/";

    private static final URI WS_ENDPOINT = URI.create("wss://ws-api.coincheck.com/");

    private static final Duration WS_INTERVAL = Duration.ofSeconds(5);

    private static final Duration TRADE_EXPIRY = Duration.ofHours(24);

    private final Object annotatedEndpoint;

    private final Gson gson;

    private final Map<String, NavigableMap<Instant, CoincheckTrade>> trades;

    private final AtomicLong lastNonce;

    private final ExecutorService executor;

    public CoincheckContext() {

        super(ID);

        GsonBuilder builder = new GsonBuilder();

        builder.registerTypeAdapter(Instant.class, new JsonDeserializer<Instant>() {

            private final DateTimeFormatter FMT = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("GMT"));

            @Override
            public Instant deserialize(JsonElement j, Type t, JsonDeserializationContext c) throws JsonParseException {

                ZonedDateTime timestamp = ZonedDateTime.parse(j.getAsString(), FMT);

                return timestamp.toInstant();

            }

        });

        annotatedEndpoint = this;

        gson = builder.create();

        trades = synchronizedMap(new HashMap<>());

        lastNonce = new AtomicLong();

        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName(getClass().getSimpleName());
            return t;
        });

    }

    @Override
    public void close() throws Exception {

        executor.shutdown();

        super.close();

    }

    @VisibleForTesting
    Optional<CoincheckTick> queryTick(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return Optional.empty();
        }

        Key newKey = Key.build(key).instrument(WILDCARD).build();

        CoincheckTick tick = findCached(CoincheckTick.class, newKey, () -> {

            String data = request(URL_TICK);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, CoincheckTick.class);

        });

        return Optional.ofNullable(tick);

    }

    @VisibleForTesting
    Optional<CoincheckBook> queryBook(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return Optional.empty();
        }

        Key newKey = Key.build(key).instrument(WILDCARD).build();

        CoincheckBook book = findCached(CoincheckBook.class, newKey, () -> {

            String data = request(URL_BOOK);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, CoincheckBook.class);

        });

        return Optional.ofNullable(book);

    }

    @Override
    public BigDecimal getBestAskPrice(Key key) {
        return queryBook(key).map(CoincheckBook::getBestAskPrice).orElse(null);
    }

    @Override
    public BigDecimal getBestBidPrice(Key key) {
        return queryBook(key).map(CoincheckBook::getBestBidPrice).orElse(null);
    }

    @Override
    public BigDecimal getBestAskSize(Key key) {
        return queryBook(key).map(CoincheckBook::getBestAskSize).orElse(null);
    }

    @Override
    public BigDecimal getBestBidSize(Key key) {
        return queryBook(key).map(CoincheckBook::getBestBidSize).orElse(null);
    }

    @Override
    public BigDecimal getLastPrice(Key key) {
        return queryTick(key).map(CoincheckTick::getLast).orElse(null);
    }

    @Override
    public Map<BigDecimal, BigDecimal> getAskPrices(Key key) {
        return queryBook(key).map(CoincheckBook::getAsks).orElseGet(() -> super.getAskPrices(key));
    }

    @Override
    public Map<BigDecimal, BigDecimal> getBidPrices(Key key) {
        return queryBook(key).map(CoincheckBook::getBids).orElseGet(() -> super.getBidPrices(key));
    }

    @Override
    public List<Trade> listTrades(Key key, Instant fromTime) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return Collections.emptyList();
        }

        NavigableMap<Instant, CoincheckTrade> map;

        synchronized (executor) {

            if (trades.isEmpty()) {

                executor.submit(() -> scheduleSocket(WS_ENDPOINT, WS_INTERVAL));

            }

            map = trades.computeIfAbsent(product.getId(), id -> new ConcurrentSkipListMap<>());

        }

        return map.values().stream()
                .filter(Objects::nonNull)
                .filter(e -> Objects.nonNull(e.getTimestamp()))
                .filter(e -> fromTime == null || !e.getTimestamp().isBefore(fromTime))
                .collect(toList());

    }

    @VisibleForTesting
    void scheduleSocket(URI uri, Duration interval) {

        Session session = null;

        while (!executor.isShutdown()) {

            try {

                if (session == null || !session.isOpen()) {

                    WebSocketContainer c = ContainerProvider.getWebSocketContainer();

                    session = c.connectToServer(annotatedEndpoint, uri);

                    log.debug("Initialized socket : {}", session.getId());

                }

            } catch (Exception e) {
                log.debug("Scheduling failure.", e);
            }

            try {
                MILLISECONDS.sleep(interval.toMillis());
            } catch (InterruptedException e) {
                log.debug("Scheduling interrupted.");
            }

        }

        IOUtils.closeQuietly(session);

    }

    @OnOpen
    public void onWebSocketOpen(Session s) throws IOException {

        log.debug("Socket opened : {}", s.getId());

        Map<String, String> request = new HashMap<>();
        request.put("type", "subscribe");
        request.put("channel", "btc_jpy-trades");

        String message = gson.toJson(request);
        s.getBasicRemote().sendText(message);

    }

    @OnError
    public void onWebSocketError(Session s, Throwable t) {

        log.debug("Socket error : " + s.getId(), t);

        IOUtils.closeQuietly(s);

    }

    @OnClose
    public void onWebSocketClose(Session s, CloseReason reason) {

        log.debug("Socket closed : {}", s.getId());

    }

    @OnMessage
    public void onWebSocketMessage(Reader message) throws IOException {

        // [id, pair, price, size, side]
        JsonReader reader = gson.newJsonReader(message);
        reader.beginArray();
        reader.skipValue();
        String pair = reader.nextString();
        String price = reader.nextString();
        String size = reader.nextString();
        reader.skipValue();
        reader.endArray();
        reader.close();

        appendCache(pair, CoincheckTrade.builder()
                .timestamp(getNow())
                .price(new BigDecimal(price))
                .size(new BigDecimal(size))
                .build());

    }

    @VisibleForTesting
    boolean appendCache(String id, CoincheckTrade trade) {

        if (trade == null || trade.getTimestamp() == null) {
            return false;
        }

        if (trade.getPrice() == null || trade.getPrice().signum() <= 0) {
            return false;
        }

        if (trade.getSize() == null || trade.getSize().signum() <= 0) {
            return false;
        }

        NavigableMap<Instant, CoincheckTrade> map = trades.get(StringUtils.trimToEmpty(id));

        if (map == null) {
            return false;
        }

        Instant timestamp = trade.getTimestamp().truncatedTo(ChronoUnit.SECONDS);

        CoincheckTrade truncated = CoincheckTrade.builder()
                .id(timestamp.getEpochSecond())
                .timestamp(timestamp)
                .price(trade.getPrice())
                .size(trade.getSize())
                .build();

        map.merge(timestamp, truncated, (t1, t2) -> {

            BigDecimal n1 = t1.getPrice().multiply(t1.getSize());
            BigDecimal n2 = t2.getPrice().multiply(t2.getSize());

            BigDecimal n = n1.add(n2);
            BigDecimal s = t1.getSize().add(t2.getSize());
            BigDecimal p = n.divide(s, SCALE, HALF_UP);

            return CoincheckTrade.builder().id(timestamp.getEpochSecond())
                    .timestamp(timestamp).price(p).size(s).build();

        });

        Instant cutoff = timestamp.minus(TRADE_EXPIRY);

        while (true) {

            Map.Entry<Instant, ?> entry = map.firstEntry();

            if (entry == null || entry.getKey().isAfter(cutoff)) {
                break;
            }

            map.remove(entry.getKey());

        }

        return true;

    }

    @Override
    public CurrencyType getInstrumentCurrency(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        return product == null ? null : product.getInstrumentCurrency();

    }

    @Override
    public CurrencyType getFundingCurrency(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        return product == null ? null : product.getFundingCurrency();

    }

    @Override
    public String findProduct(Key key, CurrencyType instrument, CurrencyType funding) {

        for (ProductType type : ProductType.values()) {

            if (instrument != type.getInstrumentCurrency()) {
                continue;
            }

            if (funding != type.getFundingCurrency()) {
                continue;
            }

            return type.name();

        }

        return null;

    }

    @Override
    public BigDecimal getConversionPrice(Key key, CurrencyType currency) {

        if (currency != null) {

            if (currency == getInstrumentCurrency(key)) {
                return ONE;
            }

            if (currency == getFundingCurrency(key)) {

                BigDecimal p = getMidPrice(key);

                return p == null ? null : p.negate();

            }

        }

        return null;

    }

    @VisibleForTesting
    String executePrivate(RequestType type, String url, Map<String, String> parameters, String data) throws Exception {

        String apiKey = getStringProperty("api.id", null);
        String secret = getStringProperty("api.secret", null);

        if (StringUtils.isEmpty(apiKey) || StringUtils.isEmpty(secret)) {
            return null;
        }

        String result;

        synchronized (lastNonce) {

            long currNonce = 0;

            while (currNonce <= lastNonce.get()) {

                TimeUnit.MILLISECONDS.sleep(1);

                currNonce = getNow().toEpochMilli();

            }

            lastNonce.set(currNonce);

            String path = url + buildQueryParameter(parameters);
            String nonce = String.valueOf(currNonce);
            String message = nonce + path + StringUtils.trimToEmpty(data);
            String hash = computeHash("HmacSHA256", secret.getBytes(), message.getBytes());

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("ACCESS-KEY", apiKey);
            headers.put("ACCESS-NONCE", nonce);
            headers.put("ACCESS-SIGNATURE", hash);

            result = request(type, path, headers, data);


        }

        return result;

    }

    @VisibleForTesting
    Optional<CoincheckBalance> queryBalance(Key key) {

        Key newKey = Key.build(key).instrument(WILDCARD).build();

        CoincheckBalance book = findCached(CoincheckBalance.class, newKey, () -> {

            String data = executePrivate(GET, URL_BALANCE, null, null);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            CoincheckBalance balance = gson.fromJson(data, CoincheckBalance.class);

            return Objects.equals(TRUE, balance.getSuccess()) ? balance : null;

        });

        return Optional.ofNullable(book);

    }

    @Override
    public BigDecimal getInstrumentPosition(Key key) {

        CurrencyType currency = getInstrumentCurrency(key);

        return getPosition(key, currency);

    }

    @Override
    public BigDecimal getFundingPosition(Key key) {

        CurrencyType currency = getFundingCurrency(key);

        return getPosition(key, currency);

    }

    private BigDecimal getPosition(Key key, CurrencyType currency) {

        if (currency == CurrencyType.BTC) {
            return queryBalance(key)
                    .filter(b -> b.getBtc() != null)
                    .filter(b -> b.getBtcReserved() != null)
                    .map(b -> b.getBtc().add(b.getBtcReserved()))
                    .orElse(null);
        }

        if (currency == CurrencyType.JPY) {
            return queryBalance(key)
                    .filter(b -> b.getJpy() != null)
                    .filter(b -> b.getJpyReserved() != null)
                    .map(b -> b.getJpy().add(b.getJpyReserved()))
                    .orElse(null);
        }

        return null;

    }

    @Override
    public BigDecimal roundLotSize(Key key, BigDecimal value, RoundingMode mode) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        if (value == null || mode == null) {
            return null;
        }

        BigDecimal lotSize = getDecimalProperty("size.lot", product.getLotSize());

        if (lotSize.signum() == 0) {
            return null;
        }

        return value.divide(lotSize, 0, mode).multiply(lotSize);

    }

    @Override
    public BigDecimal roundTickSize(Key key, BigDecimal value, RoundingMode mode) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        if (value == null || mode == null) {
            return null;
        }

        BigDecimal tickSize = getDecimalProperty("size.tick", product.getTickSize());

        if (tickSize.signum() == 0) {
            return null;
        }

        return value.divide(tickSize, 0, mode).multiply(tickSize);

    }

    @Override
    public BigDecimal getCommissionRate(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        return getDecimalProperty("commission." + product.name(), ZERO);

    }

    @VisibleForTesting
    List<CoincheckOrder> fetchOrders(Key key) {

        Key newKey = Key.build(key).instrument(WILDCARD).build();

        return listCached(CoincheckOrder.class, newKey, () -> {

            String data = executePrivate(GET, URL_ORDER_LIST, null, null);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            CoincheckOrder.Container c = gson.fromJson(data, CoincheckOrder.Container.class);

            return Objects.equals(TRUE, c.getSuccess()) ? c.getOrders() : null;

        });

    }

    @Override
    public Order findOrder(Key key, String id) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        return trimToEmpty(fetchOrders(key)).stream()
                .filter(Objects::nonNull)
                .filter(o -> o.getProduct() != null)
                .filter(o -> StringUtils.equals(o.getProduct(), product.getId()))
                .filter(o -> o.getId() != null)
                .filter(o -> StringUtils.equals(o.getId(), id))
                .findAny().orElse(null);
    }

    @Override
    public List<Order> listActiveOrders(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return emptyList();
        }

        return trimToEmpty(fetchOrders(key)).stream()
                .filter(Objects::nonNull)
                .filter(o -> o.getProduct() != null)
                .filter(o -> StringUtils.equals(o.getProduct(), product.getId()))
                .filter(o -> o.getActive() != null)
                .filter(CoincheckOrder::getActive)
                .collect(toList());
    }

    @Override
    public List<Order.Execution> listExecutions(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return emptyList();
        }

        Key newKey = Key.build(key).instrument(WILDCARD).build();

        List<CoincheckTransaction> values = listCached(CoincheckTransaction.class, newKey, () -> {

            String data = executePrivate(GET, URL_EXECUTION, null, null);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            CoincheckTransaction.Container c = gson.fromJson(data, CoincheckTransaction.Container.class);

            return Objects.equals(TRUE, c.getSuccess()) ? c.getTransactions() : null;

        });

        return trimToEmpty(values).stream()
                .filter(Objects::nonNull)
                .filter(t -> t.getProduct() != null)
                .filter(t -> t.getProduct().equals(product.getId()))
                .collect(toList());

    }

    @Override
    public Map<CreateInstruction, String> createOrders(Key key, Set<CreateInstruction> instructions) {

        Map<CreateInstruction, String> results = new IdentityHashMap<>();

        for (CreateInstruction i : trimToEmpty(instructions)) {

            if (i == null || i.getPrice() == null || i.getSize() == null || i.getSize().signum() == 0) {

                results.put(i, null);

                continue;

            }

            ProductType product = ProductType.find(key.getInstrument());

            if (product == null) {

                results.put(i, null);

                continue;

            }

            try {

                Map<String, String> body = new LinkedHashMap<>();
                body.put("pair", product.getId());
                body.put("amount", i.getSize().abs().toPlainString());

                if (i.getPrice().signum() != 0) {
                    body.put("rate", i.getPrice().toPlainString());
                    body.put("order_type", (i.getSize().signum() >= 0 ? BUY : SELL).getId());
                } else {
                    body.put("order_type", (i.getSize().signum() >= 0 ? MARKET_BUY : MARKET_SELL).getId());
                }

                String result = executePrivate(POST, URL_ORDER_CREATE, null, gson.toJson(body));

                CoincheckOrder.Response response = gson.fromJson(result, CoincheckOrder.Response.class);

                results.put(i, TRUE.equals(response.getSuccess()) ? response.getId() : null);

            } catch (Exception e) {

                log.warn("Order create failure : " + i, e);

                results.put(i, null);

            }

        }

        return results;

    }

    @Override
    public Map<CancelInstruction, String> cancelOrders(Key key, Set<CancelInstruction> instructions) {

        Map<CancelInstruction, String> results = new IdentityHashMap<>();

        trimToEmpty(instructions).stream().filter(Objects::nonNull).forEach(i -> {

            try {

                String data = executePrivate(DELETE, URL_ORDER_CANCEL + i.getId(), null, null);

                if (StringUtils.isEmpty(data)) {

                    results.put(i, null);

                } else {

                    CoincheckOrder.Response response = gson.fromJson(data, CoincheckOrder.Response.class);

                    results.put(i, TRUE.equals(response.getSuccess()) ? response.getId() : null);

                }

            } catch (Exception e) {

                log.warn("Order cancel failure : " + i, e);

                results.put(i, null);

            }

        });

        return results;

    }

}
