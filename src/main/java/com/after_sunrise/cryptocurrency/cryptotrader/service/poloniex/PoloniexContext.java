package com.after_sunrise.cryptocurrency.cryptotrader.service.poloniex;

import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class PoloniexContext extends TemplateContext implements PoloniexService {

    protected static final String URL_TICKER = "https://poloniex.com/public?command=returnTicker";

    protected static final String URL_TRADE = "https://poloniex.com/public?command=returnTradeHistory&currencyPair=";

    private static final Type TYPE_TICKER = new TypeToken<Map<String, PoloniexTick>>() {
    }.getType();

    private static final BigDecimal HALF = new BigDecimal("0.5");

    private final Gson gson;

    public PoloniexContext() {

        super(ID);

        gson = new Gson();

    }

    @VisibleForTesting
    Optional<PoloniexTick> queryTick(Key key) {

        PoloniexTick tick = findCached(PoloniexTick.class, key, () -> {

            String data = query(URL_TICKER);

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
    public BigDecimal getMidPrice(Key key) {

        BigDecimal ask = getBestAskPrice(key);

        BigDecimal bid = getBestBidPrice(key);

        if (ask == null || bid == null) {
            return null;
        }

        return ask.add(bid).multiply(HALF);

    }

    @Override
    public BigDecimal getLastPrice(Key key) {
        return queryTick(key).map(PoloniexTick::getLast).orElse(null);
    }

}
