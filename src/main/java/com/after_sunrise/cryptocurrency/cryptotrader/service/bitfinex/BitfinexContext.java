package com.after_sunrise.cryptocurrency.cryptotrader.service.bitfinex;

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
public class BitfinexContext extends TemplateContext implements BitfinexService {

    static final String URL_TICKER = "https://api.bitfinex.com/v1/pubticker/";

    static final String URL_TRADE = "https://api.bitfinex.com/v1/trades/";

    private static final Type TYPE_TRADE = new TypeToken<List<BitfinexTrade>>() {
    }.getType();

    private final Gson gson;

    public BitfinexContext() {

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
    Optional<BitfinexTick> queryTick(Key key) {

        BitfinexTick tick = findCached(BitfinexTick.class, key, () -> {

            String product = URLEncoder.encode(key.getInstrument(), UTF_8.name());

            String data = request(URL_TICKER + product);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            return gson.fromJson(data, BitfinexTick.class);

        });

        return Optional.ofNullable(tick);

    }

    @Override
    public BigDecimal getBestAskPrice(Key key) {
        return queryTick(key).map(BitfinexTick::getAsk).orElse(null);
    }

    @Override
    public BigDecimal getBestBidPrice(Key key) {
        return queryTick(key).map(BitfinexTick::getBid).orElse(null);
    }

    @Override
    public BigDecimal getLastPrice(Key key) {
        return queryTick(key).map(BitfinexTick::getLast).orElse(null);
    }

    @Override
    public List<Trade> listTrades(Key key, Instant fromTime) {

        List<BitfinexTrade> values = listCached(BitfinexTrade.class, key, () -> {

            String product = URLEncoder.encode(key.getInstrument(), UTF_8.name());

            String data = request(URL_TRADE + product);

            if (StringUtils.isEmpty(data)) {
                return null;
            }

            List<BitfinexTrade> trades = gson.fromJson(data, TYPE_TRADE);

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

        if (instrument == CurrencyType.BTC) {
            if (funding == CurrencyType.USD) {
                return "btcusd";
            }
        }

        if (instrument == CurrencyType.ETH) {
            if (funding == CurrencyType.BTC) {
                return "ethbtc";
            }
        }

        if (instrument == CurrencyType.BCH) {
            if (funding == CurrencyType.BTC) {
                return "bchbtc";
            }
        }

        return null;

    }

}
