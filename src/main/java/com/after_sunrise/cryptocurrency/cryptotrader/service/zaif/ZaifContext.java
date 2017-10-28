package com.after_sunrise.cryptocurrency.cryptotrader.service.zaif;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class ZaifContext extends TemplateContext implements ZaifService {

    static final String URL_TICKER = "https://api.zaif.jp/api/1/ticker/";

    static final String URL_TRADE = "https://api.zaif.jp/api/1/trades/";

    private static final Type TYPE_TRADE = new TypeToken<List<ZaifTrade>>() {
    }.getType();

    private final Gson gson;

    public ZaifContext() {

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
    Optional<ZaifTick> queryTick(Key key) {

        ZaifTick tick = findCached(ZaifTick.class, key, () -> {

            String product = URLEncoder.encode(key.getInstrument(), UTF_8.name());

            String data = query(URL_TICKER + product);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, ZaifTick.class);

        });

        return Optional.ofNullable(tick);

    }

    @Override
    public BigDecimal getBestAskPrice(Key key) {
        return queryTick(key).map(ZaifTick::getAsk).orElse(null);
    }

    @Override
    public BigDecimal getBestBidPrice(Key key) {
        return queryTick(key).map(ZaifTick::getBid).orElse(null);
    }

    @Override
    public BigDecimal getLastPrice(Key key) {
        return queryTick(key).map(ZaifTick::getLast).orElse(null);
    }

    @Override
    public List<Trade> listTrades(Key key, Instant fromTime) {

        List<ZaifTrade> values = listCached(ZaifTrade.class, key, () -> {

            String product = URLEncoder.encode(key.getInstrument(), UTF_8.name());

            String data = query(URL_TRADE + product);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            List<ZaifTrade> trades = gson.fromJson(data, TYPE_TRADE);

            return Collections.unmodifiableList(trades);

        });

        if (fromTime == null) {
            return new ArrayList<>(values);
        }

        return unmodifiableList(values.stream()
                .filter(e -> Objects.nonNull(e.getTimestamp()))
                .filter(e -> !e.getTimestamp().isBefore(fromTime))
                .collect(toList()));

    }

}
