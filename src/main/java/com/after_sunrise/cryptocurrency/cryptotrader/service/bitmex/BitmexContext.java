package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.getProperty;
import static java.math.BigDecimal.ZERO;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.HttpMethod.GET;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitmexContext extends TemplateContext implements BitmexService {

    static final String CONF = Paths.get(getProperty("user.home"), ".bitmex").toAbsolutePath().toString();

    static final String URL = "https://www.bitmex.com";

    static final String URL_TICKER = "/api/v1/instrument/activeAndIndices";

    static final String URL_TRADE = "/api/v1/trade?count=500&reverse=true&symbol=";

    static final String URL_POSITION = "/api/v1/position";

    static final String URL_MARGIN = "/api/v1/user/margin";

    private static final Type TYPE_TICKER = new TypeToken<List<BitmexTick>>() {
    }.getType();

    private static final Type TYPE_TRADE = new TypeToken<List<BitmexTrade>>() {
    }.getType();

    private static final Type TYPE_POSITION = new TypeToken<List<BitmexPosition>>() {
    }.getType();

    private static final Type TYPE_MARGIN = new TypeToken<List<BitmexMargin>>() {
    }.getType();

    private final Gson gson;

    public BitmexContext() throws ConfigurationException {

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

    }

    @VisibleForTesting
    Optional<BitmexTick> queryTick(Key key) {

        if (key == null) {
            return Optional.empty();
        }

        List<BitmexTick> ticks = listCached(BitmexTick.class, key, () -> {

            String data = query(URL + URL_TICKER);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, TYPE_TICKER);

        });

        if (ticks == null) {
            return Optional.empty();
        }

        return ticks.stream()
                .filter(Objects::nonNull)
                .filter(t -> StringUtils.isNotBlank(t.getSymbol()))
                .filter(t -> StringUtils.equals(t.getSymbol(), key.getInstrument()))
                .findAny();

    }

    @Override
    public BigDecimal getBestAskPrice(Key key) {
        return queryTick(key).map(BitmexTick::getAsk).orElse(null);
    }

    @Override
    public BigDecimal getBestBidPrice(Key key) {
        return queryTick(key).map(BitmexTick::getBid).orElse(null);
    }

    @Override
    public BigDecimal getMidPrice(Key key) {
        return queryTick(key).map(BitmexTick::getMid).orElse(null);
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

            String instrument = URLEncoder.encode(key.getInstrument(), UTF_8.name());

            String data = query(URL + URL_TRADE + instrument);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return Collections.unmodifiableList(gson.fromJson(data, TYPE_TRADE));

        });

        if (CollectionUtils.isEmpty(trades)) {
            return Collections.emptyList();
        }

        return trades.stream()
                .filter(Objects::nonNull)
                .filter(t -> t.getTimestamp() != null)
                .filter(t -> fromTime == null || t.getTimestamp().isAfter(fromTime))
                .collect(Collectors.toList());

    }

    @VisibleForTesting
    String prepareParameters(Map<String, String> parameters) throws IOException {

        if (MapUtils.isEmpty(parameters)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, String> entry : parameters.entrySet()) {

            if (StringUtils.isEmpty(entry.getKey())) {
                continue;
            }

            if (StringUtils.isEmpty(entry.getValue())) {
                continue;
            }

            sb.append(URLEncoder.encode(entry.getKey(), UTF_8.name()));
            sb.append('=');
            sb.append(URLEncoder.encode(entry.getValue(), UTF_8.name()));

        }

        return sb.length() == 0 ? StringUtils.EMPTY : "?" + sb.toString();

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
    String executePrivate(String url, Map<String, String> parameters, Object data) throws Exception {

        Configuration configuration = new Configurations().properties(CONF);
        String apiKey = configuration.getString("BITMEX_ID");
        String secret = configuration.getString("BITMEX_SECRET");

        if (StringUtils.isEmpty(apiKey) || StringUtils.isEmpty(secret)) {
            return null;
        }

        String suffix = prepareParameters(parameters);
        String nonce = String.valueOf(getNow().toEpochMilli());
        String body = data == null ? null : gson.toJson(data);
        String hash = computeHash(secret, GET, url + suffix, nonce, body);

        Map<String, String> headers = new HashMap<>();
        headers.put("api-key", apiKey);
        headers.put("api-nonce", nonce);
        headers.put("api-signature", hash);

        return query(URL + url + suffix, headers);

    }

    @Override
    public BigDecimal getInstrumentPosition(Key key) {

        if (key == null) {
            return null;
        }

        List<BitmexPosition> margins = listCached(BitmexPosition.class, key, () -> {

            String data = executePrivate(URL_POSITION, null, null);

            if (StringUtils.isEmpty(data)) {
                return Collections.emptyList();
            }

            return Collections.unmodifiableList(gson.fromJson(data, TYPE_POSITION));

        });

        // TODO : Convert from contract unit to funding unit

        return Optional.ofNullable(margins).orElse(emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(p -> StringUtils.equals(p.getSymbol(), key.getInstrument()))
                .findAny()
                .map(BitmexPosition::getQuantity)
                .orElse(null);

    }

    @Override
    public BigDecimal getFundingPosition(Key key) {

        if (key == null) {
            return null;
        }

        List<BitmexMargin> margins = listCached(BitmexMargin.class, key, () -> {

            String data = executePrivate(URL_MARGIN, singletonMap("currency", "all"), null);

            if (StringUtils.isEmpty(data)) {
                return Collections.emptyList();
            }

            return Collections.unmodifiableList(gson.fromJson(data, TYPE_MARGIN));

        });

        // TODO : Validate instrument code

        return Optional.ofNullable(margins).orElse(emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(m -> "XBt".equals(m.getCurrency()))
                .findAny()
                .map(BitmexMargin::getMarginBalance)
                .map(v -> v.multiply(SATOSHI))
                .orElse(null);

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

        BigDecimal maker = queryTick(key).map(BitmexTick::getMakerFee).orElse(ZERO);

        BigDecimal clear = queryTick(key).map(BitmexTick::getClearFee).orElse(ZERO);

        return maker.add(clear);

    }

    @Override
    public Boolean isMarginable(Key key) {
        return queryTick(key).map(BitmexTick::getExpiry).isPresent();
    }

    @Override
    public ZonedDateTime getExpiry(Key key) {

        Instant instant = queryTick(key).map(BitmexTick::getExpiry).orElse(null);

        if (instant == null) {
            return null;
        }

        // TODO : Externalize

        return ZonedDateTime.ofInstant(instant, ZoneId.of("GMT"));

    }

    @Override
    public Order findOrder(Key key, String id) {
        return null; // TODO
    }

    @Override
    public List<Order> listActiveOrders(Key key) {
        return null; // TODO
    }

    @Override
    public List<Order.Execution> listExecutions(Key key) {
        return null; // TODO
    }

    @Override
    public Map<Instruction.CreateInstruction, String> createOrders(Key key, Set<Instruction.CreateInstruction> instructions) {
        return null; // TODO
    }

    @Override
    public Map<Instruction.CancelInstruction, String> cancelOrders(Key key, Set<Instruction.CancelInstruction> instructions) {
        return null; // TODO
    }

}
