package com.after_sunrise.cryptocurrency.cryptotrader.service.btcbox;

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

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType.POST;
import static java.lang.Boolean.FALSE;
import static java.math.BigDecimal.ONE;
import static java.util.Collections.*;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BtcboxContext extends TemplateContext implements BtcboxService {

    private static final String ENDPOINT = "https://www.btcbox.co.jp";

    private static final DateTimeFormatter DTF = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("GMT"));

    private static final Type TYPE_TRADE = new TypeToken<List<BtcboxTrade>>() {
    }.getType();

    private static final Type TYPE_ORDER = new TypeToken<List<BtcboxOrder>>() {
    }.getType();

    private final Gson gson;

    public BtcboxContext() {

        super(ID);

        gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class,
                        (JsonDeserializer<Instant>) (j, t, c) -> Instant.ofEpochSecond(j.getAsLong())
                )
                .registerTypeAdapter(ZonedDateTime.class,
                        (JsonDeserializer<ZonedDateTime>) (j, t, c) -> ZonedDateTime.parse(j.getAsString(), DTF)
                )
                .create();

    }

    @VisibleForTesting
    Optional<BtcboxTick> fetchTick(Key key) {

        if (ProductType.BTC_JPY != ProductType.find(key.getInstrument())) {
            return null;
        }

        BtcboxTick value = findCached(BtcboxTick.class, key, () -> {

            String data = request(ENDPOINT + "/api/v1/ticker");

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, BtcboxTick.class);

        });

        return Optional.ofNullable(value);

    }

    @Override
    public BigDecimal getBestAskPrice(Key key) {
        return fetchTick(key).map(BtcboxTick::getSell).orElse(null);
    }

    @Override
    public BigDecimal getBestBidPrice(Key key) {
        return fetchTick(key).map(BtcboxTick::getBuy).orElse(null);
    }

    @Override
    public BigDecimal getLastPrice(Key key) {
        return fetchTick(key).map(BtcboxTick::getLast).orElse(null);
    }

    @VisibleForTesting
    Optional<BtcboxDepth> fetchDepth(Key key) {

        if (ProductType.BTC_JPY != ProductType.find(key.getInstrument())) {
            return null;
        }

        BtcboxDepth value = findCached(BtcboxDepth.class, key, () -> {

            String data = request(ENDPOINT + "/api/v1/depth");

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, BtcboxDepth.class);

        });

        return Optional.ofNullable(value);

    }

    @Override
    public BigDecimal getBestAskSize(Key key) {
        return fetchDepth(key).map(BtcboxDepth::getAskPrices)
                .map(NavigableMap::firstEntry).map(Map.Entry::getValue).orElse(null);
    }

    @Override
    public BigDecimal getBestBidSize(Key key) {
        return fetchDepth(key).map(BtcboxDepth::getBidPrices)
                .map(NavigableMap::firstEntry).map(Map.Entry::getValue).orElse(null);
    }

    @Override
    public Map<BigDecimal, BigDecimal> getAskPrices(Key key) {
        return fetchDepth(key).map(BtcboxDepth::getAskPrices).orElse(null);
    }

    @Override
    public Map<BigDecimal, BigDecimal> getBidPrices(Key key) {
        return fetchDepth(key).map(BtcboxDepth::getBidPrices).orElse(null);
    }

    @Override
    public List<Trade> listTrades(Key key, Instant fromTime) {

        if (ProductType.BTC_JPY != ProductType.find(key.getInstrument())) {
            return null;
        }

        List<BtcboxTrade> values = listCached(BtcboxTrade.class, key, () -> {

            String data = request(ENDPOINT + "/api/v1/orders");

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, TYPE_TRADE);

        });

        return trimToEmpty(values).stream()
                .filter(Objects::nonNull)
                .filter(v -> v.getTimestamp() != null)
                .filter(v -> fromTime == null || !v.getTimestamp().isBefore(fromTime))
                .collect(Collectors.toList());

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

        if (instrument != CurrencyType.BTC) {
            return null;
        }

        if (funding != CurrencyType.JPY) {
            return null;
        }

        return ProductType.BTC_JPY.name();

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

        return product == null ? null : product.getCommissionRate();

    }

    @Override
    public Boolean isMarginable(Key key) {
        return FALSE;
    }

    @Override
    public ZonedDateTime getExpiry(Key key) {
        return null;
    }

    @VisibleForTesting
    String post(String path, Map<String, String> parameters) throws IOException, InterruptedException {

        String apiKey = getStringProperty("api.id", null);
        String secret = getStringProperty("api.secret", null);

        if (StringUtils.isEmpty(apiKey) || StringUtils.isEmpty(secret)) {
            return null;
        }

        String result;

        synchronized (ENDPOINT) {

            // Avoid duplicate nonce
            TimeUnit.MILLISECONDS.sleep(1);

            Map<String, String> map = new LinkedHashMap<>(trimToEmpty(parameters));
            map.put("key", apiKey);
            map.put("nonce", String.valueOf(getNow().toEpochMilli()));

            String body = StringUtils.join(
                    map.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new),
                    "&"
            );

            String sign = computeHash("HmacSHA256", md5Hex(secret).getBytes(), body.getBytes());

            String data = body + "&signature=" + sign;

            Map<String, String> headers = singletonMap("Content-Type", "application/x-www-form-urlencoded");

            result = request(POST, ENDPOINT + path, headers, data);

        }

        return result;

    }

    @VisibleForTesting
    Optional<BtcboxBalance> fetchBalance(Key key) {

        Key newKey = Key.build(key).instrument(WILDCARD).build();

        BtcboxBalance value = findCached(BtcboxBalance.class, newKey, () -> {

            String data = post("/api/v1/balance", emptyMap());

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, BtcboxBalance.class);

        });

        return Optional.ofNullable(value);

    }

    @Override
    public BigDecimal getInstrumentPosition(Key key) {

        if (ProductType.BTC_JPY != ProductType.find(key.getInstrument())) {
            return null;
        }

        return fetchBalance(key).map(BtcboxBalance::getBtc).orElse(null);

    }

    @Override
    public BigDecimal getFundingPosition(Key key) {

        if (ProductType.BTC_JPY != ProductType.find(key.getInstrument())) {
            return null;
        }

        return fetchBalance(key).map(BtcboxBalance::getJpy).orElse(null);

    }

    @Override
    public BtcboxOrder findOrder(Key key, String id) {

        if (ProductType.BTC_JPY != ProductType.find(key.getInstrument())) {
            return null;
        }

        if (StringUtils.isEmpty(id)) {
            return null;
        }

        BtcboxOrder value = findCached(BtcboxOrder.class, key, () -> {

            String data = post("/api/v1/trade_view", singletonMap("id", id));

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, BtcboxOrder.class);

        });

        return value;

    }

    @Override
    public List<Order> listActiveOrders(Key key) {

        if (ProductType.BTC_JPY != ProductType.find(key.getInstrument())) {
            return emptyList();
        }

        List<BtcboxOrder> values = listCached(BtcboxOrder.class, key, () -> {

            String data = post("/api/v1/trade_list", singletonMap("type", "open"));

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, TYPE_ORDER);

        });

        return trimToEmpty(values).stream().filter(Objects::nonNull).collect(Collectors.toList());

    }

    @Override
    public List<Order.Execution> listExecutions(Key key) {

        if (ProductType.BTC_JPY != ProductType.find(key.getInstrument())) {
            return emptyList();
        }

        List<BtcboxOrder> values = listCached(BtcboxOrder.class, key, () -> {

            String data = post("/api/v1/trade_list", singletonMap("type", "all"));

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, TYPE_ORDER);

        });

        return trimToEmpty(values).stream().filter(Objects::nonNull)
                .filter(v -> v.getFilledQuantity() != null)
                .filter(v -> v.getFilledQuantity().signum() != 0)
                .map(v -> v.new BtcboxExecution())
                .collect(Collectors.toList());

    }

    @Override
    public Map<CreateInstruction, String> createOrders(Key key, Set<CreateInstruction> instructions) {

        Map<CreateInstruction, String> results = new IdentityHashMap<>();

        trimToEmpty(instructions).stream().filter(Objects::nonNull).forEach(i -> {

            if (ProductType.BTC_JPY != ProductType.find(key.getInstrument())) {

                results.put(i, null);

                return;

            }

            if (i.getPrice() == null || i.getPrice().signum() == 0
                    || i.getSize() == null || i.getSize().signum() == 0) {

                results.put(i, null);

                return;

            }

            Map<String, String> parameters = new TreeMap<>();
            parameters.put("price", i.getPrice().stripTrailingZeros().toPlainString());
            parameters.put("amount", i.getSize().abs().stripTrailingZeros().toPlainString());
            parameters.put("type", i.getSize().signum() > 0 ? "buy" : "sell");

            try {

                String data = post("/api/v1/trade_add", parameters);

                BtcboxResponse response = gson.fromJson(data, BtcboxResponse.class);

                results.put(i, response == null || !response.isResult() ? null : response.getId());

            } catch (Exception e) {

                log.warn("Order create failure : " + i, e);

                results.put(i, null);

            }

        });

        return results;

    }

    @Override
    public Map<CancelInstruction, String> cancelOrders(Key key, Set<CancelInstruction> instructions) {

        Map<CancelInstruction, String> results = new IdentityHashMap<>();

        trimToEmpty(instructions).stream().filter(Objects::nonNull).forEach(i -> {

            if (ProductType.BTC_JPY != ProductType.find(key.getInstrument())) {

                results.put(i, null);

                return;

            }

            if (StringUtils.isEmpty(i.getId())) {

                results.put(i, null);

                return;

            }

            try {

                String data = post("/api/v1/trade_cancel", singletonMap("id", i.getId()));

                BtcboxResponse response = gson.fromJson(data, BtcboxResponse.class);

                results.put(i, response == null || !response.isResult() ? null : response.getId());

            } catch (Exception e) {

                log.warn("Order cancel failure : " + i, e);

                results.put(i, null);

            }

        });

        return results;

    }

}
