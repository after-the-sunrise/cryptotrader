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
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexService.SideType.BUY;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexService.SideType.SELL;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexTick.UNLISTED;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType.GET;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitmexContext extends TemplateContext implements BitmexService {

    private static final String URL = "https://www.bitmex.com";

    private static final String URL_ALIAS = "/api/v1/instrument/activeIntervals";

    private static final String URL_TICKER = "/api/v1/instrument/activeAndIndices";

    private static final String URL_BOOK = "/api/v1/orderBook/L2";

    private static final String URL_TRADE = "/api/v1/trade";

    private static final String URL_BUCKETED = "/api/v1/trade/bucketed";

    private static final String URL_POSITION = "/api/v1/position";

    private static final String URL_MARGIN = "/api/v1/user/margin";

    private static final String URL_ORDER = "/api/v1/order";

    private static final String URL_ORDER_BULK = "/api/v1/order/bulk";

    private static final String URL_EXECUTION = "/api/v1/execution/tradeHistory";

    private static final Type TYPE_TICKER = new TypeToken<List<BitmexTick>>() {
    }.getType();

    private static final Type TYPE_BOOK = new TypeToken<List<BitmexBook>>() {
    }.getType();

    private static final Type TYPE_TRADE = new TypeToken<List<BitmexTrade>>() {
    }.getType();

    private static final Type TYPE_BUCKETED = new TypeToken<List<BitmexTrade.Bucketed>>() {
    }.getType();

    private static final Type TYPE_POSITION = new TypeToken<List<BitmexPosition>>() {
    }.getType();

    private static final Type TYPE_MARGIN = new TypeToken<List<BitmexMargin>>() {
    }.getType();

    private static final Type TYPE_ORDER = new TypeToken<List<BitmexOrder>>() {
    }.getType();

    private static final Type TYPE_EXECUTION = new TypeToken<List<BitmexExecution>>() {
    }.getType();

    private static final Duration BUCKETED = Duration.ofHours(1);

    private final AtomicLong lastNonce = new AtomicLong();

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

        String specific = getStringProperty("alias." + key.getInstrument(), null);

        if (specific != null) {
            return specific;
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

        if (ProductType.XBT == ProductType.findByName(key.getInstrument())) {

            return Optional.of(BitmexTick.builder()
                    .state(UNLISTED)
                    .symbol(key.getInstrument())
                    .timestamp(key.getTimestamp())
                    .last(ONE)
                    .settleCurrency(FundingType.XBT.getId())
                    .build()
            );

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

        Optional<BitmexTick> result = ticks.stream()
                .filter(Objects::nonNull)
                .filter(t -> StringUtils.isNotBlank(t.getSymbol()))
                .filter(t -> StringUtils.equals(t.getSymbol(), instrument))
                .findAny();

        if (ProductType.XBT_FR == ProductType.findByName(key.getInstrument())) {

            result = result.filter(t -> t.getFundingFee() != null).map(tick -> BitmexTick.builder()
                    .state(UNLISTED)
                    .symbol(key.getInstrument())
                    .timestamp(tick.getTimestamp())
                    .last(ONE.add(tick.getFundingFee()))
                    .fundingFee(tick.getFundingFee())
                    .build()
            );

        }

        return result;

    }

    @VisibleForTesting
    List<BitmexBook> queryBooks(Key key) {

        if (key == null) {
            return Collections.emptyList();
        }

        List<BitmexBook> books = listCached(BitmexBook.class, key, () -> {

            Map<String, String> parameters = new LinkedHashMap<>();
            parameters.put("symbol", convertAlias(key));
            parameters.put("depth", TEN.toPlainString());
            String path = URL + URL_BOOK + buildQueryParameter(parameters);

            String data = request(GET, path, null, null);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return Collections.unmodifiableList(gson.fromJson(data, TYPE_BOOK));

        });

        return books != null ? books : Collections.emptyList();

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
    public BigDecimal getBestAskSize(Key key) {

        if (queryTick(key).filter(t -> UNLISTED.equals(t.getState())).isPresent()) {
            return ZERO;
        }

        return queryBooks(key).stream()
                .filter(Objects::nonNull)
                .filter(b -> b.getSide() != null)
                .filter(b -> b.getPrice() != null)
                .filter(b -> b.getSize() != null)
                .filter(b -> !BitmexBook.SIDE_BUY.equals(b.getSide()))
                .min(Comparator.comparing(BitmexBook::getPrice))
                .map(BitmexBook::getSize)
                .orElse(null);

    }

    @Override
    public BigDecimal getBestBidSize(Key key) {

        if (queryTick(key).filter(t -> UNLISTED.equals(t.getState())).isPresent()) {
            return ZERO;
        }

        return queryBooks(key).stream()
                .filter(Objects::nonNull)
                .filter(b -> b.getSide() != null)
                .filter(b -> b.getPrice() != null)
                .filter(b -> b.getSize() != null)
                .filter(b -> BitmexBook.SIDE_BUY.equals(b.getSide()))
                .max(Comparator.comparing(BitmexBook::getPrice))
                .map(BitmexBook::getSize)
                .orElse(null);

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
    public Map<BigDecimal, BigDecimal> getAskPrices(Key key) {

        if (queryTick(key).filter(t -> UNLISTED.equals(t.getState())).isPresent()) {
            return emptyMap();
        }

        Map<BigDecimal, BigDecimal> values = new LinkedHashMap<>();

        queryBooks(key).stream()
                .filter(Objects::nonNull)
                .filter(b -> b.getSide() != null)
                .filter(b -> b.getPrice() != null)
                .filter(b -> b.getSize() != null)
                .filter(b -> !BitmexBook.SIDE_BUY.equals(b.getSide()))
                .forEach(b -> values.put(b.getPrice(), b.getSize()));

        return values;

    }

    @Override
    public Map<BigDecimal, BigDecimal> getBidPrices(Key key) {

        if (queryTick(key).filter(t -> UNLISTED.equals(t.getState())).isPresent()) {
            return emptyMap();
        }

        Map<BigDecimal, BigDecimal> values = new LinkedHashMap<>();

        queryBooks(key).stream()
                .filter(Objects::nonNull)
                .filter(b -> b.getSide() != null)
                .filter(b -> b.getPrice() != null)
                .filter(b -> b.getSize() != null)
                .filter(b -> BitmexBook.SIDE_BUY.equals(b.getSide()))
                .forEach(b -> values.put(b.getPrice(), b.getSize()));

        return values;

    }

    @Override
    public List<Trade> listTrades(Key key, Instant fromTime) {

        if (key == null) {
            return Collections.emptyList();
        }

        Optional<BitmexTick> tick = queryTick(key);

        if (tick
                .filter(t -> t.getLast() != null)
                .filter(t -> t.getTimestamp() != null)
                .filter(t -> UNLISTED.equals(t.getState()))
                .filter(t -> fromTime == null || !fromTime.isAfter(t.getTimestamp()))
                .isPresent()) {

            return tick.map(t -> BitmexTrade.builder()
                    .timestamp(t.getTimestamp())
                    .price(t.getLast())
                    .size(ZERO)
                    .build()
            ).map(Collections::<Trade>singletonList).orElseGet(Collections::emptyList);

        }

        List<? extends Trade> trades = listCached(BitmexTrade.class, key, () -> {

            Map<String, String> parameters = new LinkedHashMap<>();
            parameters.put("count", "500");
            parameters.put("reverse", "true");
            parameters.put("symbol", convertAlias(key));

            String path = URL + URL_TRADE + buildQueryParameter(parameters);

            String data = request(path);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return Collections.unmodifiableList(gson.fromJson(data, TYPE_TRADE));

        });

        Instant cutoff = key.getTimestamp().minus(BUCKETED);

        if (trimToEmpty(trades).stream()
                .filter(Objects::nonNull)
                .filter(t -> t.getTimestamp() != null)
                .noneMatch(t -> t.getTimestamp().isBefore(cutoff))) {

            trades = listCached(BitmexTrade.Bucketed.class, key, () -> {

                Map<String, String> parameters = new LinkedHashMap<>();
                parameters.put("binSize", "1m");
                parameters.put("partial", "true");
                parameters.put("count", "500");
                parameters.put("reverse", "true");
                parameters.put("symbol", convertAlias(key));

                String path = URL + URL_BUCKETED + buildQueryParameter(parameters);

                String data = request(path);

                if (StringUtils.isEmpty(data)) {
                    return null;
                }

                return Collections.unmodifiableList(gson.fromJson(data, TYPE_BUCKETED));

            });

        }

        return trimToEmpty(trades).stream()
                .filter(Objects::nonNull)
                .filter(t -> t.getPrice() != null)
                .filter(t -> t.getPrice().signum() != 0)
                .filter(t -> t.getSize() != null)
                .filter(t -> t.getSize().signum() != 0)
                .filter(t -> t.getTimestamp() != null)
                .filter(t -> fromTime == null || !fromTime.isAfter(t.getTimestamp()))
                .collect(toList());

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
    public String findProduct(Key key, CurrencyType instrument, CurrencyType funding) {

        if (funding == CurrencyType.USD) {
            if (instrument == CurrencyType.BTC) {
                return ProductType.BXBT.name();
            }
        }

        if (funding == CurrencyType.JPY) {
            if (instrument == CurrencyType.BTC) {
                return ProductType.BXBTJPY.name();
            }
        }

        if (funding == CurrencyType.BTC) {
            if (instrument == CurrencyType.ETH) {
                return ProductType.ETHXBT.name();
            }
            if (instrument == CurrencyType.ETC) {
                return ProductType.ETCXBT.name();
            }
            if (instrument == CurrencyType.BCH) {
                return ProductType.BCHXBT.name();
            }
        }

        return null;

    }

    @Override
    public BigDecimal getConversionPrice(Key key, CurrencyType currency) {

        ProductType product = ProductType.findByName(key.getInstrument());

        if (product == null) {
            return null;
        }

        Integer multiplier = product.getMultiplier();

        if (multiplier == null) {
            return null;
        }

        BigDecimal price = getMidPrice(key);

        if (price == null || price.signum() == 0) {
            return null;
        }

        BigDecimal result = null;

        if (product.getStructure() == currency) {

            // Contract Value = Multiplier / Contract Price

            // 1 Currency Value = 1 / Contract Value

            result = price.divide(BigDecimal.valueOf(multiplier), SCALE, HALF_UP);

        }

        if (product.getFunding() == currency) {

            // Contract Value = Multiplier * Contract Price

            // 1 Currency Value = 1 / Contract Value

            result = ONE.divide(BigDecimal.valueOf(multiplier).multiply(price), SCALE, HALF_UP);

        }

        return result;

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

        String suffix = buildQueryParameter(parameters);

        String result;

        synchronized (lastNonce) {

            long millis = 0;

            for (int i = 0; i < Short.MAX_VALUE; i++) {

                millis = getNow().toEpochMilli();

                if (lastNonce.get() < millis) {

                    lastNonce.set(millis);

                    break;

                }

                Thread.yield();

            }

            String nonce = String.valueOf(millis);
            String hash = computeHash(secret, type.name(), url + suffix, nonce, data);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("api-key", apiKey);
            headers.put("api-nonce", nonce);
            headers.put("api-signature", hash);

            result = request(type, URL + url + suffix, headers, data);

        }

        return result;

    }

    @Override
    public BigDecimal getInstrumentPosition(Key key) {

        if (key == null) {
            return null;
        }

        if (ProductType.XBT == ProductType.findByName(key.getInstrument())) {
            return getMarginAmount(key, BitmexMargin::getMarginBalance);
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
        return getMarginAmount(key, BitmexMargin::getExcess);
    }

    @VisibleForTesting
    BigDecimal getMarginAmount(Key key, Function<BitmexMargin, BigDecimal> function) {

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
                .map(function)
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

            BigDecimal maker = tick.map(BitmexTick::getMakerFee).orElse(ZERO).max(ZERO);

            BigDecimal clear = tick.map(BitmexTick::getSettleFee).orElse(ZERO).max(ZERO);

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

        return trimToEmpty(values);

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

        return trimToEmpty(values).stream().filter(Objects::nonNull).collect(toList());

    }

    @Override
    public Map<CreateInstruction, String> createOrders(Key key, Set<CreateInstruction> instructions) {

        if (CollectionUtils.isEmpty(instructions)) {
            return Collections.emptyMap();
        }

        List<CreateInstruction> inputs = instructions.stream().filter(Objects::nonNull).collect(toList());

        Map<CreateInstruction, String> map = new IdentityHashMap<>();

        try {

            Map<CreateInstruction, String> ids = new IdentityHashMap<>();

            String data = gson.toJson(singletonMap("orders", inputs.stream()
                    .filter(i -> i.getPrice() != null)
                    .filter(i -> i.getPrice().signum() != 0)
                    .filter(i -> i.getSize() != null)
                    .filter(i -> i.getSize().signum() != 0)
                    .map(i -> {
                        ids.put(i, getUniqueId());
                        Map<String, Object> params = new TreeMap<>();
                        params.put("symbol", convertAlias(key));
                        params.put("side", (i.getSize().signum() >= 0 ? BUY : SELL).getId());
                        params.put("orderQty", i.getSize().abs());
                        params.put("price", i.getPrice());
                        params.put("clOrdID", ids.get(i));
                        params.put("ordType", i.getPrice().signum() == 0 ? "Market" : "Limit");
                        params.put("execInst", i.getStrategy());
                        return params;
                    }).collect(toList())
            ));

            String result = executePrivate(RequestType.POST, URL_ORDER_BULK, emptyMap(), data);

            List<BitmexOrder> results = gson.fromJson(result, TYPE_ORDER);

            inputs.forEach(i -> map.put(i, results.stream()
                    .filter(Objects::nonNull)
                    .filter(o -> StringUtils.isNotEmpty(o.getClientId()))
                    .filter(o -> StringUtils.equals(o.getClientId(), ids.get(i)))
                    .map(BitmexOrder::getClientId)
                    .findAny().orElse(null))
            );

        } catch (Exception e) {

            log.warn("Order create failure : " + instructions, e);

            instructions.stream().filter(Objects::nonNull).forEach(i -> map.put(i, null));

        }

        return map;

    }

    @Override
    public Map<CancelInstruction, String> cancelOrders(Key key, Set<CancelInstruction> instructions) {

        if (CollectionUtils.isEmpty(instructions)) {
            return Collections.emptyMap();
        }

        Map<CancelInstruction, String> map = new IdentityHashMap<>();

        try {

            String data = gson.toJson(singletonMap("clOrdID", instructions.stream()
                    .filter(Objects::nonNull)
                    .map(CancelInstruction::getId)
                    .collect(toList())
            ));

            String result = executePrivate(RequestType.DELETE, URL_ORDER, emptyMap(), data);

            List<BitmexOrder> results = gson.fromJson(result, TYPE_ORDER);

            instructions.stream().filter(Objects::nonNull).forEach(i -> {

                String id = results.stream()
                        .filter(Objects::nonNull)
                        .filter(o -> StringUtils.isNotEmpty(o.getClientId()))
                        .filter(o -> StringUtils.equals(o.getClientId(), i.getId()))
                        .map(BitmexOrder::getClientId)
                        .findAny().orElse(null);

                map.put(i, id);

            });

        } catch (Exception e) {

            log.warn("Order cancel failure : " + instructions, e);

            instructions.stream().filter(Objects::nonNull).forEach(i -> map.put(i, null));

        }

        return map;

    }

}
