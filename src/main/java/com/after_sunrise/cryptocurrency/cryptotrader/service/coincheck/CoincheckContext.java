package com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class CoincheckContext extends TemplateContext implements CoincheckService {

    static final String PRODUCT = "btc_jpy";

    static final String URL_TICKER = "https://coincheck.com/api/ticker";

    static final String URL_TRADE = "https://coincheck.com/api/trades";

    private static final Type TYPE_TRADE = new TypeToken<List<CoincheckTrade>>() {
    }.getType();

    private final Gson gson;

    public CoincheckContext() {

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
    Optional<CoincheckTick> queryTick(Key key) {

        if (!PRODUCT.equals(key.getInstrument())) {
            return Optional.empty();
        }

        CoincheckTick tick = findCached(CoincheckTick.class, key, () -> {

            String data = request(URL_TICKER);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, CoincheckTick.class);

        });

        return Optional.ofNullable(tick);

    }

    @Override
    public BigDecimal getBestAskPrice(Key key) {
        return queryTick(key).map(CoincheckTick::getAsk).orElse(null);
    }

    @Override
    public BigDecimal getBestBidPrice(Key key) {
        return queryTick(key).map(CoincheckTick::getBid).orElse(null);
    }

    @Override
    public BigDecimal getLastPrice(Key key) {
        return queryTick(key).map(CoincheckTick::getLast).orElse(null);
    }

    @Override
    public List<Trade> listTrades(Key key, Instant fromTime) {

        if (!PRODUCT.equals(key.getInstrument())) {
            return Collections.emptyList();
        }

        List<CoincheckTrade> values = listCached(CoincheckTrade.class, key, () -> {

            String data = request(URL_TRADE);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            List<CoincheckTrade> trades = gson.fromJson(data, TYPE_TRADE);

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
