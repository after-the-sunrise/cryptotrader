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
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType.POST;
import static java.lang.Boolean.FALSE;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitpointContext extends TemplateContext implements BitpointService {

    private static final String URL_SMART = "https://smartapi.bitpoint.co.jp/bpj-smart-api";

    private static final String URL_CLASSIC = "https://public.bitpoint.co.jp/bpj-api";

    private static final String ACCESS_TOKEN = "access_token";

    private static final Type TYPE_TRADE = new TypeToken<List<BitpointTrade>>() {
    }.getType();

    private static final Type TYPE_TOKEN = new TypeToken<Map<String, String>>() {
    }.getType();

    private final Gson gson;

    private final Lock lock;

    private final AtomicReference<Pair<Instant, String>> token;

    public BitpointContext() {

        super(ID);

        GsonBuilder builder = new GsonBuilder();

        builder.registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (j, t, c) -> {

            BigDecimal timestamp = j.getAsBigDecimal();

            return Instant.ofEpochMilli(timestamp.longValue());

        });

        gson = builder.create();

        lock = new ReentrantLock();

        token = new AtomicReference<>();

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
    Future<String> requestAsync(String path, String body) {

        String apiKey = getStringProperty("api.id", null);
        String secret = getStringProperty("api.secret", null);

        if (StringUtils.isEmpty(apiKey) || StringUtils.isEmpty(secret)) {
            return null;
        }

        CompletableFuture<String> future = new CompletableFuture<>();

        try {

            lock.lock();

            Pair<Instant, String> accessToken = token.get();

            Long millis = getLongProperty("api.expire", TimeUnit.MINUTES.toMillis(59));

            Instant now = getNow();

            if (accessToken == null || accessToken.getLeft().isBefore(now.minusMillis(millis))) {

                Map<String, String> parameters = new LinkedHashMap<>();
                parameters.put("username", apiKey);
                parameters.put("password", secret);

                String json = request(URL_CLASSIC + "/login" + buildQueryParameter(parameters, "?"));

                accessToken = Pair.of(now, MapUtils.getString(gson.fromJson(json, TYPE_TOKEN), ACCESS_TOKEN));

                token.set(accessToken);

            }

            // Requests must be serial (Error Code : E_BL_0023)

            Map<String, String> headers = singletonMap("Content-Type", "application/json");

            String query = buildQueryParameter(singletonMap(ACCESS_TOKEN, accessToken.getRight()), "?");

            future.complete(request(POST, URL_CLASSIC + path + query, headers, body));

        } catch (Exception e) {

            token.set(null); // TODO: Clear token only on authentication failure.

            future.completeExceptionally(e);

        } finally {

            lock.unlock();

        }

        return future;

    }

    @Override
    public BigDecimal getInstrumentPosition(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        BitpointCoinBalance balance = findCached(BitpointCoinBalance.class, key, () -> {

            Map<String, Object> body = new LinkedHashMap<>();

            body.put("calcCurrencyCd", product.getFundingCurrency().name());

            body.put("currencyCdList", singletonList(product.getInstrumentCurrency().name()));

            String data = extract(requestAsync("/vc_balance_list", gson.toJson(body)));

            BitpointCoinBalance result = gson.fromJson(data, BitpointCoinBalance.class);

            return result != null && BitpointCoinBalance.SUCCESS.equals(result.getResult()) ? result : null;

        });

        if (balance == null) {
            return null;
        }

        return balance.getBalances().stream()
                .filter(Objects::nonNull)
                .map(BitpointCoinBalance.Balance::getAmount)
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);

    }

    @Override
    public BigDecimal getFundingPosition(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        BitpointCashBalance balance = findCached(BitpointCashBalance.class, key, () -> {

            Map<String, Object> body = new LinkedHashMap<>();

            body.put("currencyCdList", singletonList(product.getFundingCurrency().name()));

            String data = extract(requestAsync("/rc_balance_list", gson.toJson(body)));

            BitpointCashBalance result = gson.fromJson(data, BitpointCashBalance.class);

            return result != null && BitpointCashBalance.SUCCESS.equals(result.getResult()) ? result : null;

        });

        if (balance == null) {
            return null;
        }

        return balance.getBalances().stream()
                .filter(Objects::nonNull)
                .map(BitpointCashBalance.Balance::getAmount)
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);

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
            params.put("currencyCd1", product.getInstrumentCurrency().name());
            params.put("currencyCd2", product.getFundingCurrency().name());
            params.put("buySellClsSearch", "0"); // 0:All, 1:Sell, 3:Buy
            params.put("orderStatus", "2"); // 0:All, 2:Active, ...
            params.put("period", "5"); // 0:TD, 1:T-1, 2:1M, 3:3M, 4:6M, 5:1Y
            params.put("refTradeTypeCls", "0"); // 0:All, 1:Open, 2:Close, 3:Cash

            String data = extract(requestAsync("/vc_order_refer_list", gson.toJson(params)));

            BitpointOrder.Container c = gson.fromJson(data, BitpointOrder.Container.class);

            return c == null ? null : c.getOrders();

        });

        if (orders == null) {
            return null;
        }

        return orders.stream().filter(Objects::nonNull).collect(toList());

    }

    @Override
    public List<Execution> listExecutions(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        List<BitpointExecution> executions = listCached(BitpointExecution.class, key, () -> {

            Map<String, String> params = new LinkedHashMap<>();
            params.put("currencyCd1", product.getInstrumentCurrency().name());
            params.put("currencyCd2", product.getFundingCurrency().name());
            params.put("buySellClsSearch", "0"); // 0:All, 1:Sell, 3:Buy
            params.put("period", "0"); // 0:TD, 1:T-1, 2:1M, 3:3M, 4:6M, 5:1Y
            params.put("refTradeTypeCls", "0"); // 0:All, 1:Open, 2:Close, 3:Cash

            String data = extract(requestAsync("/vc_contract_refer_list", gson.toJson(params)));

            BitpointExecution.Container c = gson.fromJson(data, BitpointExecution.Container.class);

            return c == null ? null : c.getExecutions();

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
            params.put("tradingPassword", getStringProperty("api.pin", null));
            params.put("buySellCls", i.getSize().signum() >= 0 ? "3" : "1");
            params.put("orderNominal", i.getSize().abs().toPlainString());
            params.put("currencyCd1", product.getInstrumentCurrency().name());
            params.put("currencyCd2", product.getFundingCurrency().name());
            params.put("conditionCls", i.getPrice().signum() == 0 ? "2" : "1");
            params.put("orderPriceIn", i.getPrice().toPlainString());
            params.put("durationCls", "1");

            return requestAsync("/spot_order", gson.toJson(params));

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
            params.put("tradingPassword", getStringProperty("api.pin", null));
            params.put("orderNo", i.getId());

            return requestAsync("/spot_order_cancel", gson.toJson(params));

        }, d -> {

            BitpointOrder o = gson.fromJson(d, BitpointOrder.class);

            return o == null ? null : o.getId();

        });

    }

}
