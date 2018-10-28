package com.after_sunrise.cryptocurrency.cryptotrader.service.fisco;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order.Execution;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType.POST;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.Collections.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class FiscoContext extends TemplateContext implements FiscoService {

    static final String URL_DEPTH = "https://api.fcce.jp/api/1/depth/";

    static final String URL_TRADE = "https://api.fcce.jp/api/1/trades/";

    static final String URL_POST = "https://api.fcce.jp/tapi";

    private static final Type TYPE_TRADE = new TypeToken<List<FiscoTrade>>() {
    }.getType();

    private final Gson gson;

    public FiscoContext() {

        super(ID);

        GsonBuilder builder = new GsonBuilder();

        builder.registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (j, t, c) -> {

            BigDecimal unixTime = j.getAsBigDecimal();

            long timestamp = unixTime.movePointRight(3).longValue();

            return Instant.ofEpochMilli(timestamp);

        });

        gson = builder.create();

    }

    @VisibleForTesting
    Optional<FiscoDepth> queryDepth(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        FiscoDepth value = findCached(FiscoDepth.class, key, () -> {

            String data = request(URL_DEPTH + product.getId());

            return gson.fromJson(data, FiscoDepth.class);

        });

        return Optional.ofNullable(value);

    }

    @Override
    public BigDecimal getBestAskPrice(Key key) {
        return queryDepth(key).map(FiscoDepth::getAskPrices)
                .map(NavigableMap::firstEntry).map(Entry::getKey).orElse(null);
    }

    @Override
    public BigDecimal getBestBidPrice(Key key) {
        return queryDepth(key).map(FiscoDepth::getBidPrices)
                .map(NavigableMap::firstEntry).map(Entry::getKey).orElse(null);
    }

    @Override
    public BigDecimal getBestAskSize(Key key) {
        return queryDepth(key).map(FiscoDepth::getAskPrices)
                .map(NavigableMap::firstEntry).map(Entry::getValue).orElse(null);
    }

    @Override
    public BigDecimal getBestBidSize(Key key) {
        return queryDepth(key).map(FiscoDepth::getBidPrices)
                .map(NavigableMap::firstEntry).map(Entry::getValue).orElse(null);
    }

    @Override
    public Map<BigDecimal, BigDecimal> getAskPrices(Key key) {
        return queryDepth(key).map(FiscoDepth::getAskPrices).orElse(null);
    }

    @Override
    public Map<BigDecimal, BigDecimal> getBidPrices(Key key) {
        return queryDepth(key).map(FiscoDepth::getBidPrices).orElse(null);
    }

    @Override
    public BigDecimal getLastPrice(Key key) {

        List<Trade> trades = listTrades(key, null);

        return trimToEmpty(trades).stream()
                .filter(Objects::nonNull)
                .filter(t -> t.getTimestamp() != null)
                .max(Comparator.comparing(Trade::getTimestamp))
                .map(Trade::getPrice)
                .orElse(null);

    }

    @Override
    public List<Trade> listTrades(Key key, Instant fromTime) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        List<FiscoTrade> values = listCached(FiscoTrade.class, key, () -> {

            String data = request(URL_TRADE + product.getId());

            List<FiscoTrade> trades = gson.fromJson(data, TYPE_TRADE);

            return Collections.unmodifiableList(trades);

        });

        if (values == null) {
            return null;
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
    public String findProduct(Key key, CurrencyType instrument, CurrencyType funding) {

        for (ProductType product : ProductType.values()) {

            if (product.getInstrumentCurrency() != instrument) {
                continue;
            }

            if (product.getFundingCurrency() != funding) {
                continue;
            }

            return product.name();

        }

        return null;

    }

    @Override
    public BigDecimal getConversionPrice(Key key, CurrencyType currency) {

        if (currency == null) {
            return null;
        }

        if (currency == getInstrumentCurrency(key)) {
            return ONE;
        }

        if (currency == getFundingCurrency(key)) {

            BigDecimal p = getMidPrice(key);

            return p == null ? null : p.negate();

        }

        return null;

    }

    @VisibleForTesting
    Future<String> postAsync(String method, Map<String, String> parameters) throws Exception {

        String apiKey = getStringProperty("api.id", null);
        String secret = getStringProperty("api.secret", null);

        if (StringUtils.isEmpty(apiKey) || StringUtils.isEmpty(secret)) {
            return null;
        }

        Future<String> result;

        synchronized (URL_POST) {

            // Avoid duplicate nonce
            MILLISECONDS.sleep(5);

            Map<String, String> map = new LinkedHashMap<>(trimToEmpty(parameters));
            map.put("nonce", BigDecimal.valueOf(getNow().toEpochMilli()).movePointLeft(3).toPlainString());
            map.put("method", method);
            String data = buildQueryParameter(map, "");

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("key", apiKey);
            headers.put("sign", computeHash("HmacSHA512", secret.getBytes(), data.getBytes()));

            result = requestAsync(POST, URL_POST, headers, data);

        }

        return result;

    }

    @VisibleForTesting
    Optional<FiscoBalance> fetchBalance(Key key) {

        Key newKey = Key.build(key).instrument(WILDCARD).build();

        FiscoBalance balance = findCached(FiscoBalance.class, newKey, () -> {

            String data = extract(postAsync("get_info", emptyMap()));

            FiscoBalance.Container c = gson.fromJson(data, FiscoBalance.Container.class);

            if (!c.isSuccess()) {
                throw new IOException("Invalid balance : " + c);
            }

            return c.getBalance();

        });

        return Optional.ofNullable(balance);

    }

    @Override
    public BigDecimal getInstrumentPosition(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        return fetchBalance(key).map(FiscoBalance::getDeposits)
                .map(m -> m.get(product.getInstrumentCurrency().name().toLowerCase())).orElse(null);

    }

    @Override
    public BigDecimal getFundingPosition(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        return fetchBalance(key).map(FiscoBalance::getDeposits)
                .map(m -> m.get(product.getFundingCurrency().name().toLowerCase())).orElse(null);

    }

    @Override
    public BigDecimal roundLotSize(Key key, BigDecimal value, RoundingMode mode) {

        ProductType product = ProductType.find(key.getInstrument());

        return product == null ? null : super.round(value, mode, product.getLotSize());

    }

    @Override
    public BigDecimal roundTickSize(Key key, BigDecimal value, RoundingMode mode) {

        ProductType product = ProductType.find(key.getInstrument());

        return product == null ? null : super.round(value, mode, product.getTickSize());

    }

    @Override
    public BigDecimal getCommissionRate(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        return getDecimalProperty("commission." + product.name(), ZERO);

    }

    @Override
    public Boolean isMarginable(Key key) {
        return FALSE;
    }

    @Override
    public Order findOrder(Key key, String id) {

        if (StringUtils.isEmpty(id)) {
            return null;
        }

        List<Order> orders = listActiveOrders(key);

        return trimToEmpty(orders).stream()
                .filter(Objects::nonNull)
                .filter(o -> StringUtils.equals(o.getId(), id))
                .findFirst().orElse(null);

    }

    @Override
    public List<Order> listActiveOrders(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        List<FiscoOrder> orders = listCached(FiscoOrder.class, key, () -> {

            Map<String, String> parameters = singletonMap("currency_pair", product.getId());

            String data = extract(postAsync("active_orders", parameters));

            FiscoOrder.Container c = gson.fromJson(data, FiscoOrder.Container.class);

            if (!c.isSuccess()) {
                throw new IOException("Invalid orders " + c);
            }

            return unmodifiableList(trimToEmpty(c.getOrders()).entrySet().stream()
                    .map(e -> FiscoOrder.builder().id(e.getKey()).data(e.getValue()).build())
                    .collect(toList()));

        });

        if (orders == null) {
            return null;
        }

        return orders.stream()
                .filter(Objects::nonNull)
                .filter(o -> Objects.equals(TRUE, o.getActive()))
                .collect(toList());

    }

    @Override
    public List<Execution> listExecutions(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        List<FiscoExecution> executions = listCached(FiscoExecution.class, key, () -> {

            Map<String, String> parameters = new LinkedHashMap<>();
            parameters.put("currency_pair", product.getId());
            parameters.put("count", "100");

            String data = extract(postAsync("trade_history", parameters));

            FiscoExecution.Container c = gson.fromJson(data, FiscoExecution.Container.class);

            if (!c.isSuccess()) {
                throw new IOException("Invalid executions : " + c);
            }

            return unmodifiableList(trimToEmpty(c.getExecutions()).entrySet().stream()
                    .map(e -> FiscoExecution.builder().id(e.getKey()).data(e.getValue()).build())
                    .collect(toList()));

        });

        if (executions == null) {
            return null;
        }

        return executions.stream().filter(Objects::nonNull).collect(toList());

    }

    @Override
    public Map<CreateInstruction, String> createOrders(Key key, Set<CreateInstruction> instructions) {

        ProductType product = ProductType.find(key.getInstrument());

        return handleCreates(instructions, i -> {

            if (product == null) {
                return CompletableFuture.completedFuture((String) null);
            }

            Map<String, String> parameters = new IdentityHashMap<>();
            parameters.put("currency_pair", product.getId());
            parameters.put("action", i.getSize().signum() > 0 ? "bid" : "ask");
            parameters.put("price", i.getPrice().toPlainString());
            parameters.put("amount", i.getSize().abs().toPlainString());

            return postAsync("trade", parameters);

        }, d -> {

            FiscoOrder.Response r = gson.fromJson(d, FiscoOrder.Response.class);

            return r.getOrderId();

        });

    }

    @Override
    public Map<CancelInstruction, String> cancelOrders(Key key, Set<CancelInstruction> instructions) {

        ProductType product = ProductType.find(key.getInstrument());

        return handleCancels(instructions, i -> {

            if (product == null) {
                return CompletableFuture.completedFuture((String) null);
            }

            Map<String, String> parameters = new IdentityHashMap<>();
            parameters.put("currency_pair", product.getId());
            parameters.put("order_id", i.getId());

            return postAsync("cancel_order", parameters);

        }, data -> {

            FiscoOrder.Response r = gson.fromJson(data, FiscoOrder.Response.class);

            return r.getOrderId();

        });

    }

}
