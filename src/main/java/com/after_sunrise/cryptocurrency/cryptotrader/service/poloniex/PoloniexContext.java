package com.after_sunrise.cryptocurrency.cryptotrader.service.poloniex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class PoloniexContext extends TemplateContext implements PoloniexService {

    static final String URL_TICKER = "https://poloniex.com/public?command=returnTicker";

    static final String URL_TRADE = "https://poloniex.com/public?command=returnTradeHistory&currencyPair=";

    private static final Type TYPE_TICKER = new TypeToken<Map<String, PoloniexTick>>() {
    }.getType();

    private static final Type TYPE_TRADE = new TypeToken<List<PoloniexTrade>>() {
    }.getType();

    private final Gson gson;

    public PoloniexContext() {

        super(ID);

        GsonBuilder builder = new GsonBuilder();

        builder.registerTypeAdapter(Instant.class, new JsonDeserializer<Instant>() {

            private final DateTimeFormatter FMT = ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("GMT"));

            @Override
            public Instant deserialize(JsonElement j, Type t, JsonDeserializationContext c) throws JsonParseException {

                ZonedDateTime timestamp = ZonedDateTime.parse(j.getAsString(), FMT);

                return timestamp.toInstant();

            }

        });

        gson = builder.create();

    }

    @VisibleForTesting
    Optional<PoloniexTick> queryTick(Key key) {

        PoloniexTick tick = findCached(PoloniexTick.class, key, () -> {

            String data = request(URL_TICKER);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            Map<String, PoloniexTick> ticks = gson.fromJson(data, TYPE_TICKER);

            return ticks.get(key.getInstrument());

        });

        return Optional.ofNullable(tick);

    }

    @Override
    public BigDecimal getBestAskPrice(Key key) {
        return queryTick(key).map(PoloniexTick::getAsk).orElse(null);
    }

    @Override
    public BigDecimal getBestBidPrice(Key key) {
        return queryTick(key).map(PoloniexTick::getBid).orElse(null);
    }

    @Override
    public BigDecimal getLastPrice(Key key) {
        return queryTick(key).map(PoloniexTick::getLast).orElse(null);
    }

    @Override
    public List<Trade> listTrades(Key key, Instant fromTime) {

        List<PoloniexTrade> values = listCached(PoloniexTrade.class, key, () -> {

            String product = URLEncoder.encode(key.getInstrument(), StandardCharsets.UTF_8.name());

            String data = request(URL_TRADE + product);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            List<PoloniexTrade> trades = gson.fromJson(data, TYPE_TRADE);

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
    public String findProduct(Key key, CurrencyType instrument, CurrencyType funding) {

        if (funding == CurrencyType.USD) {
            if (instrument == CurrencyType.BTC) {
                return "USDT_BTC";
            }
        }

        if (funding == CurrencyType.BTC) {
            if (instrument == CurrencyType.ETH) {
                return "BTC_ETH";
            }
            if (instrument == CurrencyType.BCH) {
                return "BTC_BCH";
            }
        }

        return null;

    }

}
