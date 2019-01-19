package com.after_sunrise.cryptocurrency.cryptotrader.service.quoinex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType.*;
import static java.lang.Boolean.FALSE;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.stream.Collectors.toList;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class QuoinexContext extends TemplateContext implements QuoinexService {

    private static final String ENDPOINT = "https://api.liquid.com";

    private static final String PAGE_LIMIT = "1000";

    private static final Type TYPE_PRODUCT = new TypeToken<List<QuoinexProduct>>() {
    }.getType();

    private static final Type TYPE_ACCOUNT = new TypeToken<List<QuoinexAccount>>() {
    }.getType();

    private final Gson gson;

    private final String jwtHead;

    public QuoinexContext() {

        super(ID);

        gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class,
                        (JsonDeserializer<Instant>) (j, t, c) -> Instant.ofEpochSecond(j.getAsLong())
                )
                .registerTypeAdapter(BigDecimal.class,
                        (JsonDeserializer<BigDecimal>) (j, t, c) -> StringUtils.isEmpty(j.getAsString()) ? null : j.getAsBigDecimal()
                )
                .create();

        jwtHead = Base64.getUrlEncoder().encodeToString(gson.toJson(Stream.of(
                new SimpleEntry<>("typ", "JWT"),
                new SimpleEntry<>("alg", "HS256")
        ).collect(Collectors.toMap(Entry::getKey, Entry::getValue))).getBytes());

    }

    @VisibleForTesting
    protected Optional<QuoinexProduct> fetchProduct(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return Optional.empty();
        }

        Key newKey = Key.build(key).instrument(WILDCARD).build();

        List<QuoinexProduct> value = listCached(QuoinexProduct.class, newKey, () -> {

            String data = request(ENDPOINT + "/products");

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, TYPE_PRODUCT);

        });

        return trimToEmpty(value).stream()
                .filter(Objects::nonNull)
                .filter(p -> StringUtils.isNotEmpty(p.getId()))
                .filter(p -> StringUtils.isNotEmpty(p.getCode()))
                .filter(p -> StringUtils.equals(p.getCode(), product.getCode()))
                .findFirst();

    }

    protected Optional<QuoinexBook> fetchBook(Key key) {

        return fetchProduct(key).map(product -> findCached(QuoinexBook.class, key, () -> {

            String data = request(ENDPOINT + "/products/" + product.getId() + "/price_levels");

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, QuoinexBook.class);

        }));

    }

    @Override
    public BigDecimal getBestAskPrice(Key key) {
        return fetchBook(key).map(QuoinexBook::getAskPrices)
                .map(NavigableMap::firstEntry).map(Entry::getKey).orElse(null);
    }

    @Override
    public BigDecimal getBestBidPrice(Key key) {
        return fetchBook(key).map(QuoinexBook::getBidPrices)
                .map(NavigableMap::firstEntry).map(Entry::getKey).orElse(null);
    }

    @Override
    public BigDecimal getBestAskSize(Key key) {
        return fetchBook(key).map(QuoinexBook::getAskPrices)
                .map(NavigableMap::firstEntry).map(Entry::getValue).orElse(null);
    }

    @Override
    public BigDecimal getBestBidSize(Key key) {
        return fetchBook(key).map(QuoinexBook::getBidPrices)
                .map(NavigableMap::firstEntry).map(Entry::getValue).orElse(null);
    }

    @Override
    public Map<BigDecimal, BigDecimal> getAskPrices(Key key) {
        return fetchBook(key).map(QuoinexBook::getAskPrices).orElse(null);
    }

    @Override
    public Map<BigDecimal, BigDecimal> getBidPrices(Key key) {
        return fetchBook(key).map(QuoinexBook::getBidPrices).orElse(null);
    }

    @Override
    public BigDecimal getLastPrice(Key key) {
        return fetchProduct(key).map(QuoinexProduct::getLastPrice).orElse(null);
    }

    @Override
    public List<Trade> listTrades(Key key, Instant fromTime) {

        List<QuoinexTrade> trades = fetchProduct(key).map(product -> {

            QuoinexTrade.Container container = findCached(QuoinexTrade.Container.class, key, () -> {

                Map<String, String> parameters = new LinkedHashMap<>();
                parameters.put("product_id", product.getId());
                parameters.put("limit", PAGE_LIMIT);
                String queryParameter = buildQueryParameter(parameters);

                String data = request(ENDPOINT + "/executions" + queryParameter);

                if (StringUtils.isEmpty(data)) {
                    return null;
                }

                return gson.fromJson(data, QuoinexTrade.Container.class);

            });

            return container.getTrades();

        }).orElseGet(Collections::emptyList);

        return trimToEmpty(trades).stream()
                .filter(Objects::nonNull)
                .filter(t -> t.getTimestamp() != null)
                .filter(t -> fromTime == null || !t.getTimestamp().isBefore(fromTime))
                .collect(toList());

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
    String fetchPrivate(RequestType type, String path, Map<String, String> parameters, Object data) throws Exception {

        String key = getStringProperty("api.id", null);

        if (StringUtils.isEmpty(key)) {
            return null;
        }

        String secret = getStringProperty("api.secret", null);

        if (StringUtils.isEmpty(secret)) {
            return null;
        }

        String result;

        synchronized (ENDPOINT) {

            String parameter = buildQueryParameter(parameters);

            Map<String, String> jwt = new TreeMap<>();
            jwt.put("nonce", String.valueOf(getNow().toEpochMilli()));
            jwt.put("path", path + parameter);
            jwt.put("token_id", key);
            String jwtLoad = Base64.getUrlEncoder().encodeToString(gson.toJson(jwt).getBytes());

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal((jwtHead + "." + jwtLoad).getBytes());
            String jwtSign = Base64.getUrlEncoder().encodeToString(hash);

            Map<String, String> headers = new TreeMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("X-Quoine-API-Version", "2");
            headers.put("X-Quoine-Auth", jwtHead + "." + jwtLoad + "." + jwtSign);

            String json = data != null ? gson.toJson(data) : null;

            result = request(type, ENDPOINT + path + parameter, headers, json);

            TimeUnit.MILLISECONDS.sleep(1L); // Avoid duplicate nonce

        }

        return result;

    }

    @VisibleForTesting
    BigDecimal fetchBalance(Key key, Function<ProductType, CurrencyType> f) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        CurrencyType currency = f.apply(product);

        if (currency == null) {
            return null;
        }

        Key newKey = Key.build(key).instrument(WILDCARD).build();

        List<QuoinexAccount> values = listCached(QuoinexAccount.class, newKey, () -> {

            String data = fetchPrivate(GET, "/accounts/balance", null, null);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, TYPE_ACCOUNT);

        });

        return trimToEmpty(values).stream()
                .filter(Objects::nonNull)
                .filter(v -> StringUtils.isNotEmpty(v.getCurrency()))
                .filter(v -> StringUtils.equals(v.getCurrency(), currency.name()))
                .map(QuoinexAccount::getBalance)
                .findFirst().orElse(null);

    }

    @Override
    public BigDecimal getInstrumentPosition(Key key) {
        return fetchBalance(key, ProductType::getInstrumentCurrency);
    }

    @Override
    public BigDecimal getFundingPosition(Key key) {
        return fetchBalance(key, ProductType::getFundingCurrency);
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

        return fetchProduct(key).map(product -> {

            BigDecimal taker = trimToZero(product.getTakerFee());

            BigDecimal maker = trimToZero(product.getMakerFee());

            return taker.max(maker).max(ZERO);

        }).orElse(null);

    }

    @Override
    public Boolean isMarginable(Key key) {
        return FALSE;
    }

    @Override
    public QuoinexOrder findOrder(Key key, String id) {

        if (StringUtils.isEmpty(id)) {
            return null;
        }

        Key newKey = Key.build(key).instrument(key.getInstrument() + "@" + id).build();

        return findCached(QuoinexOrder.class, newKey, () -> {

            String data = fetchPrivate(GET, "/orders/" + id, null, null);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, QuoinexOrder.class);

        });

    }

    @Override
    public List<Order> listActiveOrders(Key key) {

        List<QuoinexOrder> orders = fetchProduct(key).map(product -> listCached(QuoinexOrder.class, key, () -> {

            Map<String, String> parameters = new TreeMap<>();
            parameters.put("product_id", product.getId());
            parameters.put("status", "live");

            String data = fetchPrivate(GET, "/orders", parameters, null);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, QuoinexOrder.Container.class).getValues();

        })).orElseGet(Collections::emptyList);

        return trimToEmpty(orders).stream().filter(Objects::nonNull).collect(toList());

    }

    @Override
    public List<Order.Execution> listExecutions(Key key) {

        List<QuoinexExecution> values = fetchProduct(key).map(product -> listCached(QuoinexExecution.class, key, () -> {

            Map<String, String> parameters = Collections.singletonMap("product_id", product.getId());

            String data = fetchPrivate(GET, "/executions/me", parameters, null);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, QuoinexExecution.Container.class).getValues();

        })).orElseGet(Collections::emptyList);

        return trimToEmpty(values).stream().filter(Objects::nonNull).collect(toList());

    }

    @Override
    public Map<CreateInstruction, String> createOrders(Key key, Set<CreateInstruction> instructions) {

        String id = fetchProduct(key).map(QuoinexProduct::getId).orElse(null);

        if (StringUtils.isEmpty(id)) {
            return null;
        }

        Map<CreateInstruction, String> ids = new IdentityHashMap<>();

        for (CreateInstruction i : trimToEmpty(instructions)) {

            if (i == null || i.getPrice() == null || i.getSize() == null || i.getSize().signum() == 0) {

                ids.put(i, null);

                continue;

            }

            try {

                Map<String, String> parameters = new TreeMap<>();
                parameters.put("product_id", id);
                parameters.put("order_type", i.getPrice().signum() == 0 ? "market" : "limit");
                parameters.put("price", i.getPrice().signum() == 0 ? null : i.getPrice().toPlainString());
                parameters.put("side", i.getSize().signum() > 0 ? "buy" : "sell");
                parameters.put("quantity", i.getSize().abs().toPlainString());

                String data = fetchPrivate(POST, "/orders", null, parameters);

                QuoinexOrder order = gson.fromJson(data, QuoinexOrder.class);

                ids.put(i, order.getId());

            } catch (Exception e) {

                log.warn("Order create failure : " + i, e);

                ids.put(i, null);

            }

        }

        return ids;

    }

    @Override
    public Map<CancelInstruction, String> cancelOrders(Key key, Set<CancelInstruction> instructions) {

        Map<CancelInstruction, String> ids = new IdentityHashMap<>();

        for (CancelInstruction i : trimToEmpty(instructions)) {

            if (i == null || StringUtils.isEmpty(i.getId())) {

                ids.put(i, null);

                continue;

            }

            try {

                String data = fetchPrivate(PUT, "/orders/" + i.getId() + "/cancel", null, null);

                QuoinexOrder order = gson.fromJson(data, QuoinexOrder.class);

                ids.put(i, order.getId());

            } catch (Exception e) {

                log.warn("Order cancel failure : " + i, e);

                ids.put(i, null);

            }

        }

        return ids;

    }

}
