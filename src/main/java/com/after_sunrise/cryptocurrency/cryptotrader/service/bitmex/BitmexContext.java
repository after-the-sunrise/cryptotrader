package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexService.SideType.BUY;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexService.SideType.SELL;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType.GET;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.*;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitmexContext extends TemplateContext implements BitmexService {

    static final String URL = "https://www.bitmex.com";

    static final String URL_ALIAS = "/api/v1/instrument/activeIntervals";

    static final String URL_TICKER = "/api/v1/instrument/activeAndIndices";

    static final String URL_TRADE = "/api/v1/trade?count=500&reverse=true&symbol=";

    static final String URL_POSITION = "/api/v1/position";

    static final String URL_MARGIN = "/api/v1/user/margin";

    static final String URL_ORDER = "/api/v1/order";

    static final String URL_EXECUTION = "/api/v1/execution/tradeHistory";

    private static final Type TYPE_TICKER = new TypeToken<List<BitmexTick>>() {
    }.getType();

    private static final Type TYPE_TRADE = new TypeToken<List<BitmexTrade>>() {
    }.getType();

    private static final Type TYPE_POSITION = new TypeToken<List<BitmexPosition>>() {
    }.getType();

    private static final Type TYPE_MARGIN = new TypeToken<List<BitmexMargin>>() {
    }.getType();

    private static final Type TYPE_ORDER = new TypeToken<List<BitmexOrder>>() {
    }.getType();

    private static final Type TYPE_EXECUTION = new TypeToken<List<BitmexExecution>>() {
    }.getType();

    private static final Duration TIMEOUT = Duration.ofMinutes(3);

    private static final String UNLISTED = "Unlisted";

    private final Gson gson;

    public BitmexContext() throws ConfigurationException {

        super(ID);

        GsonBuilder builder = new GsonBuilder();

        builder.registerTypeAdapter(Instant.class,
                (JsonDeserializer<Instant>) (j, t, c) -> Instant.parse(j.getAsString())
        );

        gson = builder.create();

    }

    @VisibleForTesting
    String convertAlias(Key key) {

        if (key == null) {
            return null;
        }

        ProductType product = ProductType.findByName(key.getInstrument());

        if (product == null) {
            return null;
        }

        if (product.getId() != null) {
            return product.getId();
        }

        Key newKey = Key.build(key).instrument(WILDCARD).build();

        BitmexAlias alias = findCached(BitmexAlias.class, newKey, () -> {

            String data = request(GET, URL + URL_ALIAS, null, null);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, BitmexAlias.class);

        });

        return alias == null ? null : alias.find(product.getAlias());

    }

    @VisibleForTesting
    Optional<BitmexTick> queryTick(Key key) {

        if (key == null) {
            return Optional.empty();
        }

        Key newKey = Key.build(key).instrument(WILDCARD).build();

        List<BitmexTick> ticks = listCached(BitmexTick.class, newKey, () -> {

            String data = request(GET, URL + URL_TICKER, null, null);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, TYPE_TICKER);

        });

        if (ticks == null) {
            return Optional.empty();
        }

        String instrument = convertAlias(key);

        return ticks.stream()
                .filter(Objects::nonNull)
                .filter(t -> StringUtils.isNotBlank(t.getSymbol()))
                .filter(t -> StringUtils.equals(t.getSymbol(), instrument))
                .findAny();

    }

    @Override
    public BigDecimal getBestAskPrice(Key key) {
        return queryTick(key).map(t -> UNLISTED.equals(t.getState()) ? t.getLast() : t.getAsk()).orElse(null);
    }

    @Override
    public BigDecimal getBestBidPrice(Key key) {
        return queryTick(key).map(t -> UNLISTED.equals(t.getState()) ? t.getLast() : t.getBid()).orElse(null);
    }

    @Override
    public BigDecimal getMidPrice(Key key) {
        return queryTick(key).map(t -> UNLISTED.equals(t.getState()) ? t.getLast() : t.getMid()).orElse(null);
    }

    @Override
    public BigDecimal getLastPrice(Key key) {
        return queryTick(key).map(BitmexTick::getLast).orElse(null);
    }

    @Override
    public List<Trade> listTrades(Key key, Instant fromTime) {

        if (key == null) {
            return Collections.emptyList();
        }

        List<BitmexTrade> trades = listCached(BitmexTrade.class, key, () -> {

            String instrument = convertAlias(key);

            String data = request(URL + URL_TRADE + instrument);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return Collections.unmodifiableList(gson.fromJson(data, TYPE_TRADE));

        });

        return ofNullable(trades).orElse(emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(t -> t.getTimestamp() != null)
                .filter(t -> fromTime == null || !fromTime.isAfter(t.getTimestamp()))
                .collect(toList());

    }

    @VisibleForTesting
    String computeHash(String secret, String method, String path, String nonce, String data) throws IOException {

        try {

            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));

            String raw = method + path + nonce + StringUtils.trimToEmpty(data);

            byte[] hash = mac.doFinal(raw.getBytes());

            StringBuilder sb = new StringBuilder(hash.length);

            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }

            return sb.toString();

        } catch (GeneralSecurityException e) {

            throw new IOException("Failed to compute hash.", e);

        }

    }

    @VisibleForTesting
    String executePrivate(RequestType type, String url, Map<String, String> parameters, String data) throws Exception {

        String apiKey = getStringProperty("api.id", null);
        String secret = getStringProperty("api.secret", null);

        if (StringUtils.isEmpty(apiKey) || StringUtils.isEmpty(secret)) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        if (MapUtils.isNotEmpty(parameters)) {

            for (Map.Entry<String, String> entry : parameters.entrySet()) {

                if (StringUtils.isEmpty(entry.getKey())) {
                    continue;
                }

                if (StringUtils.isEmpty(entry.getValue())) {
                    continue;
                }

                sb.append(sb.length() == 0 ? "?" : "&");
                sb.append(URLEncoder.encode(entry.getKey(), UTF_8.name()));
                sb.append('=');
                sb.append(URLEncoder.encode(entry.getValue(), UTF_8.name()));

            }

        }

        String suffix = sb.toString();
        String nonce = String.valueOf(getNow().toEpochMilli());
        String hash = computeHash(secret, type.name(), url + suffix, nonce, data);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("api-key", apiKey);
        headers.put("api-nonce", nonce);
        headers.put("api-signature", hash);

        return request(type, URL + url + suffix, headers, data);

    }

    @Override
    public CurrencyType getInstrumentCurrency(Key key) {

        ProductType product = key == null ? null : ProductType.findByName(key.getInstrument());

        return product == null ? null : product.getStructure();

    }

    @Override
    public CurrencyType getFundingCurrency(Key key) {

        ProductType product = key == null ? null : ProductType.findByName(key.getInstrument());

        return product == null ? null : product.getFunding();

    }

    @Override
    public BigDecimal getInstrumentPosition(Key key) {

        if (key == null) {
            return null;
        }

        Key newKey = Key.build(key).instrument(WILDCARD).build();

        List<BitmexPosition> positions = listCached(BitmexPosition.class, newKey, () -> {

            String data = executePrivate(GET, URL_POSITION, null, null);

            if (StringUtils.isEmpty(data)) {
                return Collections.emptyList();
            }

            return Collections.unmodifiableList(gson.fromJson(data, TYPE_POSITION));

        });

        if (CollectionUtils.isEmpty(positions)) {
            return null;
        }

        String instrument = convertAlias(key);

        return positions.stream()
                .filter(Objects::nonNull)
                .filter(p -> StringUtils.equals(p.getSymbol(), instrument))
                .findAny()
                .map(BitmexPosition::getQuantity)
                .orElse(ZERO);

    }

    @Override
    public BigDecimal getFundingPosition(Key key) {

        String currency = queryTick(key).map(BitmexTick::getSettleCurrency).orElse(null);

        BigDecimal conversionRate = getFundingConversionRate(key, FundingType.findById(currency));

        if (conversionRate == null) {
            return null;
        }

        Key newKey = Key.build(key).instrument(WILDCARD).build();

        List<BitmexMargin> margins = listCached(BitmexMargin.class, newKey, () -> {

            Map<String, String> parameters = singletonMap("currency", "all");

            String data = executePrivate(GET, URL_MARGIN, parameters, null);

            if (StringUtils.isEmpty(data)) {
                return Collections.emptyList();
            }

            return Collections.unmodifiableList(gson.fromJson(data, TYPE_MARGIN));

        });

        if (CollectionUtils.isEmpty(margins)) {
            return null;
        }

        return margins.stream()
                .filter(Objects::nonNull)
                .filter(m -> StringUtils.equals(currency, m.getCurrency()))
                .findAny()
                .map(BitmexMargin::getBalance)
                .map(v -> v.multiply(SATOSHI).multiply(conversionRate))
                .orElse(ZERO);

    }

    @VisibleForTesting
    BigDecimal getFundingConversionRate(Key key, FundingType funding) {

        if (key == null || funding == null) {
            return null;
        }

        ProductType product = ProductType.findByName(key.getInstrument());

        if (product == null) {
            return null;
        }

        if (product.getMultiplier() == null) {
            return null;
        }

        if (product.getFunding() == funding.getCurrency()) {

            // Contract value = multiplier * price

            // Eligible contracts = fund / value

            // Eligible notional = price * contracts

            // = price * [fund / (multiplier * price)] = fund / multiplier

            BigDecimal multiplier = BigDecimal.valueOf(product.getMultiplier());

            return ONE.divide(multiplier, SCALE, HALF_UP);

        }

        if (product.getStructure() == funding.getCurrency()) {

            BigDecimal price = getMidPrice(key);

            if (price == null || price.signum() == 0) {
                return null;
            }

            // Contract value = multiplier / price

            // Eligible contracts = fund / value

            // Eligible notional = price * contracts

            // = price * [fund / (multiplier / price)] = price * fund / multiplier * price

            BigDecimal multiplier = BigDecimal.valueOf(product.getMultiplier());

            return price.multiply(price).divide(multiplier, SCALE, HALF_UP);

        }

        return null;

    }

    @Override
    public BigDecimal roundLotSize(Key key, BigDecimal value, RoundingMode mode) {

        if (value == null || mode == null) {
            return null;
        }

        BigDecimal lotSize = queryTick(key).map(BitmexTick::getLotSize).orElse(null);

        if (lotSize == null || lotSize.signum() == 0) {
            return null;
        }

        BigDecimal lots = value.divide(lotSize, INTEGER_ZERO, mode);

        return lots.multiply(lotSize);

    }

    @Override
    public BigDecimal roundTickSize(Key key, BigDecimal value, RoundingMode mode) {

        if (value == null || mode == null) {
            return null;
        }

        BigDecimal tickSize = queryTick(key).map(BitmexTick::getTickSize).orElse(null);

        if (tickSize == null || tickSize.signum() == 0) {
            return null;
        }

        BigDecimal ticks = value.divide(tickSize, INTEGER_ZERO, mode);

        if (ticks == null || ticks.signum() == 0) {
            return null;
        }

        return ticks.multiply(tickSize);

    }

    @Override
    public BigDecimal getCommissionRate(Key key) {

        Optional<BitmexTick> tick = queryTick(key);

        BigDecimal rate = null;

        if (tick.isPresent()) {

            BigDecimal maker = tick.map(BitmexTick::getMakerFee).orElse(ZERO);

            BigDecimal clear = tick.map(BitmexTick::getSettleFee).orElse(ZERO);

            rate = maker.add(clear);

        }

        return rate;

    }

    @Override
    public Boolean isMarginable(Key key) {
        return Boolean.TRUE;
    }

    @Override
    public ZonedDateTime getExpiry(Key key) {
        return queryTick(key)
                .map(BitmexTick::getExpiry)
                .map(i -> ZonedDateTime.ofInstant(i, ZoneId.of("GMT")))
                .orElse(null);
    }

    @VisibleForTesting
    List<BitmexOrder> findOrders(Key key) {

        List<BitmexOrder> values = listCached(BitmexOrder.class, key, () -> {

            Map<String, String> parameters = new LinkedHashMap<>();
            parameters.put("reverse", "true");
            parameters.put("count", "500");
            parameters.put("symbol", convertAlias(key));

            String data = executePrivate(GET, URL_ORDER, parameters, null);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return Collections.unmodifiableList(gson.fromJson(data, TYPE_ORDER));

        });

        return ofNullable(values).orElse(emptyList());

    }

    @Override
    public Order findOrder(Key key, String id) {
        return findOrders(key).stream()
                .filter(Objects::nonNull)
                .filter(o -> StringUtils.isNotEmpty(id))
                .filter(o -> StringUtils.equalsAny(id, o.getOrderId(), o.getClientId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Order> listActiveOrders(Key key) {
        return findOrders(key).stream()
                .filter(Objects::nonNull)
                .filter(o -> o.getActive() != null)
                .filter(BitmexOrder::getActive)
                .collect(toList());
    }

    @Override
    public List<Order.Execution> listExecutions(Key key) {

        List<BitmexExecution> values = listCached(BitmexExecution.class, key, () -> {

            Map<String, String> parameters = new LinkedHashMap<>();
            parameters.put("count", "500");
            parameters.put("reverse", "true");
            parameters.put("symbol", convertAlias(key));

            String data = executePrivate(GET, URL_EXECUTION, parameters, null);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return Collections.unmodifiableList(gson.fromJson(data, TYPE_EXECUTION));

        });

        return ofNullable(values).orElse(emptyList()).stream()
                .filter(Objects::nonNull).collect(toList());

    }

    @Override
    public Map<CreateInstruction, String> createOrders(Key key, Set<CreateInstruction> instructions) {

        if (CollectionUtils.isEmpty(instructions)) {
            return Collections.emptyMap();
        }

        Map<CreateInstruction, CompletableFuture<String>> futures = new IdentityHashMap<>();

        instructions.stream().filter(Objects::nonNull).forEach(i -> {

            futures.put(i, CompletableFuture.supplyAsync(() -> {

                if (i.getPrice() == null || i.getPrice().signum() == 0) {
                    return null;
                }

                if (i.getSize() == null || i.getSize().signum() == 0) {
                    return null;
                }

                try {

                    Map<String, Object> data = new TreeMap<>();
                    data.put("symbol", convertAlias(key));
                    data.put("side", (i.getSize().signum() >= 0 ? BUY : SELL).getId());
                    data.put("orderQty", i.getSize().abs().toPlainString());
                    data.put("price", i.getPrice().toPlainString());
                    data.put("clOrdID", getUniqueId());
                    data.put("ordType", "Limit");
                    data.put("execInst", "ParticipateDoNotInitiate");

                    String result = executePrivate(RequestType.POST, URL_ORDER, emptyMap(), gson.toJson(data));

                    BitmexOrder results = gson.fromJson(result, BitmexOrder.class);

                    return results.getClientId();

                } catch (Exception e) {

                    throw new IllegalArgumentException("Create failure : " + i, e);

                }

            }));

        });

        Map<CreateInstruction, String> results = new IdentityHashMap<>();

        futures.forEach((k, v) -> results.put(k, extractQuietly(v, TIMEOUT)));

        return results;

    }

    @Override
    public Map<CancelInstruction, String> cancelOrders(Key key, Set<CancelInstruction> instructions) {

        if (CollectionUtils.isEmpty(instructions)) {
            return Collections.emptyMap();
        }

        Map<CancelInstruction, CompletableFuture<String>> futures = new IdentityHashMap<>();

        instructions.stream().filter(Objects::nonNull).forEach(i -> {

            futures.put(i, CompletableFuture.supplyAsync(() -> {

                try {

                    String data = gson.toJson(singletonMap("clOrdID", i.getId()));

                    String result = executePrivate(RequestType.DELETE, URL_ORDER, emptyMap(), data);

                    List<BitmexOrder> results = gson.fromJson(result, TYPE_ORDER);

                    return results.stream().findAny().map(BitmexOrder::getClientId).orElse(null);

                } catch (Exception e) {

                    throw new IllegalArgumentException("Cancel failure : " + i, e);

                }

            }));

        });

        Map<CancelInstruction, String> results = new IdentityHashMap<>();

        futures.forEach((k, v) -> results.put(k, extractQuietly(v, TIMEOUT)));

        return results;

    }

}
