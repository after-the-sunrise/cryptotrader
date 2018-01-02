package com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.*;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck.CoincheckService.SideType.*;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType.*;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ONE;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class CoincheckContext extends TemplateContext implements CoincheckService {

    private static final String URL_TICK = "https://coincheck.com/api/ticker";

    private static final String URL_BOOK = "https://coincheck.com/api/order_books";

    private static final String URL_TRADE = "https://coincheck.com/api/trades";

    private static final String URL_BALANCE = "https://coincheck.com/api/accounts/balance";

    private static final String URL_ORDER_LIST = "https://coincheck.com/api/exchange/orders/opens";

    private static final String URL_EXECUTION = "https://coincheck.com/api/exchange/orders/transactions";

    private static final String URL_ORDER_CREATE = "https://coincheck.com/api/exchange/orders";

    private static final String URL_ORDER_CANCEL = "https://coincheck.com/api/exchange/orders/";

    private static final long TRADE_LIMIT = 64;

    private static final Duration TRADE_EXPIRY = Duration.ofHours(24);

    private final Gson gson;

    private final Map<String, NavigableMap<Long, CoincheckTrade>> trades;

    private final AtomicLong lastNonce;

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

        gson = builder.create();

        trades = Collections.synchronizedMap(new HashMap<>());

        lastNonce = new AtomicLong();

    }

    @VisibleForTesting
    Optional<CoincheckTick> queryTick(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return Optional.empty();
        }

        CoincheckTick tick = findCached(CoincheckTick.class, key, () -> {

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

        CoincheckBook book = findCached(CoincheckBook.class, key, () -> {

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
    public List<Trade> listTrades(Key key, Instant fromTime) {


        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return Collections.emptyList();
        }

        List<CoincheckTrade> values;

        String id = product.getId();

        Instant cutoff = trim(key.getTimestamp(), getNow()).minus(TRADE_EXPIRY);

        NavigableMap<Long, CoincheckTrade> map = trades.computeIfAbsent(id, i -> new TreeMap<>());

        synchronized (map) {

            values = listCached(CoincheckTrade.class, key, () -> {

                for (long i = 1; i <= TRADE_LIMIT; i++) {

                    Map<String, String> parameters = new LinkedHashMap<>();
                    parameters.put("pair", id);
                    parameters.put("limit", "500");

                    if (!map.isEmpty() && i != TRADE_LIMIT) {
                        parameters.put("order", "asc");
                        parameters.put("ending_before", map.lastEntry().getKey().toString());
                    }

                    String data = request(URL_TRADE + buildQueryParameter(parameters));

                    if (StringUtils.isEmpty(data)) {
                        break;
                    }

                    CoincheckTrade.Container container = gson.fromJson(data, CoincheckTrade.Container.class);

                    List<CoincheckTrade> trades = trimToEmpty(container.getTrades()).stream()
                            .filter(Objects::nonNull)
                            .filter(trade -> trade.getId() != null)
                            .filter(trade -> trade.getTimestamp() != null)
                            .filter(trade -> trade.getTimestamp().isAfter(cutoff))
                            .filter(trade -> trade.getTimestamp().isBefore(key.getTimestamp()))
                            .collect(toList());

                    if (trades.isEmpty()) {
                        break;
                    }

                    trades.forEach(t -> map.put(t.getId(), t));

                    while (!map.isEmpty()) {

                        Map.Entry<Long, CoincheckTrade> entry = map.firstEntry();

                        if (entry.getValue().getTimestamp().isAfter(cutoff)) {
                            break;
                        }

                        map.remove(entry.getKey());

                    }

                }

                return new ArrayList<>(map.values());

            });

        }

        if (fromTime == null) {
            return new ArrayList<>(values);
        }

        return unmodifiableList(values.stream()
                .filter(e -> Objects.nonNull(e.getTimestamp()))
                .filter(e -> !e.getTimestamp().isBefore(fromTime))
                .collect(toList()));

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
    public BigDecimal getConversionPrice(Key key, CurrencyType currency) {

        if (currency != null) {

            if (currency == getInstrumentCurrency(key)) {
                return ONE;
            }

            if (currency == getFundingCurrency(key)) {
                return getMidPrice(key);
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
    public BigDecimal getFundingPosition(Key key) {

        CurrencyType currency = getFundingCurrency(key);

        if (currency == CurrencyType.BTC) {
            return queryBalance(key).map(CoincheckBalance::getBtc).orElse(null);
        }

        if (currency == CurrencyType.JPY) {
            return queryBalance(key).map(CoincheckBalance::getJpy).orElse(null);
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

        return getDecimalProperty("commission.rate", product.getCommissionRate());

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

                results.put(i, null);

            }

        });

        return results;

    }

}
