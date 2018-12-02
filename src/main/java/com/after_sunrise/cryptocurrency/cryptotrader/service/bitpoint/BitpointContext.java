package com.after_sunrise.cryptocurrency.cryptotrader.service.bitpoint;

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
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType.GET;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType.POST;
import static com.google.common.base.MoreObjects.firstNonNull;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitpointContext extends TemplateContext implements BitpointService {

    private static final String URL_SMART = "https://smartapi.bitpoint.co.jp/bpj-smart-api";

    private static final Type TYPE_TRADE = new TypeToken<List<BitpointTrade>>() {
    }.getType();

    private final Lock lock;

    private final Gson gson;

    public BitpointContext() {

        super(ID);

        GsonBuilder builder = new GsonBuilder();

        builder.registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (j, t, c) -> {

            BigDecimal timestamp = j.getAsBigDecimal();

            return Instant.ofEpochMilli(timestamp.longValue());

        });

        gson = builder.create();

        lock = new ReentrantLock();

    }

    @VisibleForTesting
    Optional<BitpointDepth> queryDepth(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        BitpointDepth value = findCached(BitpointDepth.class, key, () -> {

            Map<String, String> parameters = singletonMap("symbol", product.getId());

            String data = request(URL_SMART + "/api/depth" + buildQueryParameter(parameters));

            return gson.fromJson(data, BitpointDepth.class);

        });

        return Optional.ofNullable(value);

    }

    @Override
    public BigDecimal getBestAskPrice(Key key) {
        return queryDepth(key).map(BitpointDepth::getAskPrices)
                .map(NavigableMap::firstEntry).map(Entry::getKey).orElse(null);
    }

    @Override
    public BigDecimal getBestBidPrice(Key key) {
        return queryDepth(key).map(BitpointDepth::getBidPrices)
                .map(NavigableMap::firstEntry).map(Entry::getKey).orElse(null);
    }

    @Override
    public BigDecimal getBestAskSize(Key key) {
        return queryDepth(key).map(BitpointDepth::getAskPrices)
                .map(NavigableMap::firstEntry).map(Entry::getValue).orElse(null);
    }

    @Override
    public BigDecimal getBestBidSize(Key key) {
        return queryDepth(key).map(BitpointDepth::getBidPrices)
                .map(NavigableMap::firstEntry).map(Entry::getValue).orElse(null);
    }

    @Override
    public Map<BigDecimal, BigDecimal> getAskPrices(Key key) {
        return queryDepth(key).map(BitpointDepth::getAskPrices).orElse(null);
    }

    @Override
    public Map<BigDecimal, BigDecimal> getBidPrices(Key key) {
        return queryDepth(key).map(BitpointDepth::getBidPrices).orElse(null);
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

        List<BitpointTrade> values = listCached(BitpointTrade.class, key, () -> {

            Map<String, String> parameters = singletonMap("symbol", product.getId());

            String data = request(URL_SMART + "/api/trades" + buildQueryParameter(parameters));

            List<BitpointTrade> trades = gson.fromJson(data, TYPE_TRADE);

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
    Future<String> requestAsync(String path, Map<String, String> parameter, Map<String, String> body) throws Exception {

        String apiKey = getStringProperty("api.id", null);
        String secret = getStringProperty("api.secret", null);
        String pinCode = getStringProperty("api.pin", null);

        if (StringUtils.isEmpty(apiKey) || StringUtils.isEmpty(secret)) {
            return null;
        }

        Future<String> result;

        try {

            lock.lock();

            MILLISECONDS.sleep(5); // Avoid duplicate nonce

            String nonce = String.valueOf(getNow().toEpochMilli());

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("access-key", apiKey);
            headers.put("access-nonce", nonce);

            if (MapUtils.isEmpty(body)) {

                Map<String, String> copy = new LinkedHashMap<>(MapUtils.emptyIfNull(parameter));
                copy.put("timestamp", nonce);

                String param = buildQueryParameter(copy, "");
                String input = String.join("\n", apiKey, nonce, param);
                headers.put("access-signature", computeHash("HmacSHA256", secret.getBytes(), input.getBytes()));

                result = requestAsync(GET, URL_SMART + path + "?" + param, headers, null);

            } else {

                Map<String, String> copy = new LinkedHashMap<>(MapUtils.emptyIfNull(body));
                copy.put("timestamp", nonce);
                copy.put("pinCode", pinCode);

                String param = gson.toJson(copy);
                String input = String.join("\n", apiKey, nonce, param);
                headers.put("access-signature", computeHash("HmacSHA256", secret.getBytes(), input.getBytes()));
                headers.put("Content-Type", "application/json");

                result = requestAsync(POST, URL_SMART + path, headers, param);

            }

        } finally {
            lock.unlock();
        }

        return result;

    }

    @VisibleForTesting
    Optional<BitpointBalance> fetchBalance(Key key) {

        Key newKey = Key.build(key).instrument(WILDCARD).build();

        BitpointBalance balance = findCached(BitpointBalance.class, newKey, () -> {

            Map<String, String> param = singletonMap("timestamp", null);

            String data = extract(requestAsync("/api/account", param, null));

            return gson.fromJson(data, BitpointBalance.class);

        });

        return Optional.ofNullable(balance);

    }

    @Override
    public BigDecimal getInstrumentPosition(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        List<BitpointBalance.CoinBalance> balances = fetchBalance(key)
                .map(BitpointBalance::getCoinBalances).orElseGet(Collections::emptyList);

        return balances.stream()
                .filter(Objects::nonNull)
                .filter(b -> product.getInstrumentCurrency().name().equals(b.getAsset()))
                .findAny()
                .map(b -> firstNonNull(b.getFree(), ZERO).add(firstNonNull(b.getLocked(), ZERO)))
                .orElse(null);

    }

    @Override
    public BigDecimal getFundingPosition(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        List<BitpointBalance.CashBalance> balances = fetchBalance(key)
                .map(BitpointBalance::getCashBalances).orElseGet(Collections::emptyList);

        for (BitpointBalance.CashBalance cb : balances) {

            if (cb == null) {
                continue;
            }

            for (BitpointBalance.Asset asset : ListUtils.emptyIfNull(cb.getSpotAssets())) {

                if (asset == null) {
                    continue;
                }

                if (!product.getFundingCurrency().name().equals(asset.getAsset())) {
                    continue;
                }

                return asset.getFree();

            }

        }

        return null;

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

        List<BitpointOrder> orders = listCached(BitpointOrder.class, key, () -> {

            Map<String, String> params = new LinkedHashMap<>();
            params.put("timestamp", null);
            params.put("symbol", product.getId());
            params.put("tradeType", "SPOT");

            String data = extract(requestAsync("/api/openOrders", params, null));

            BitpointOrder.Container c = gson.fromJson(data, BitpointOrder.Container.class);

            return c == null ? null : c.getOrders();

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

        List<BitpointExecution> executions = listCached(BitpointExecution.class, key, () -> {

            Map<String, String> params = new LinkedHashMap<>();
            params.put("timestamp", null);
            params.put("symbol", product.getId());
            params.put("tradeType", "SPOT");

            String data = extract(requestAsync("/api/myTrades", params, null));

            BitpointExecution.Container c = gson.fromJson(data, BitpointExecution.Container.class);

            return c == null ? null : c.getTrades();

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

            Map<String, String> params = new LinkedHashMap<>();
            params.put("symbol", product.getId());
            params.put("side", i.getSize().signum() >= 0 ? "BUY" : "SELL");
            params.put("type", i.getPrice().signum() == 0 ? "MARKET" : "LIMIT");
            params.put("quantity", i.getSize().abs().toPlainString());
            params.put("price", i.getPrice().toPlainString());
            params.put("timestamp", null);
            params.put("pinCode", null);

            return requestAsync("/api/order/test", null, params);

        }, d -> {

            BitpointOrder o = gson.fromJson(d, BitpointOrder.class);

            return o == null ? null : o.getId();

        });

    }

    @Override
    public Map<CancelInstruction, String> cancelOrders(Key key, Set<CancelInstruction> instructions) {

        ProductType product = ProductType.find(key.getInstrument());

        return handleCancels(instructions, i -> {

            if (product == null) {
                return CompletableFuture.completedFuture((String) null);
            }

            Map<String, String> params = new LinkedHashMap<>();
            params.put("symbol", product.getId());
            params.put("orderId", i.getId());
            params.put("timestamp", null);
            params.put("pinCode", null);

            return requestAsync("/api/cancelOrder", null, params);

        }, d -> {

            BitpointOrder o = gson.fromJson(d, BitpointOrder.class);

            return o == null ? null : o.getId();

        });

    }

}
