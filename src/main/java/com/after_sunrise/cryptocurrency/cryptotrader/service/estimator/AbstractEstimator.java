package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.impl.AbstractService;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public abstract class AbstractEstimator extends AbstractService implements Estimator {

    protected static final Estimation BAIL = Estimation.builder().confidence(ZERO).build();

    // Source Site -> Source Product -> Target Site -> Target Product
    private static final Map<CurrencyType, Map<CurrencyType, Map<String, String>>> DEFAULTS = new IdentityHashMap<>();

    static {

        for (CurrencyType structure : CurrencyType.values()) {

            Map<CurrencyType, Map<String, String>> m = new IdentityHashMap<>();

            for (CurrencyType funding : CurrencyType.values()) {
                m.put(funding, new HashMap<>());
            }

            DEFAULTS.put(structure, m);

        }

        // Catch Unknown
        DEFAULTS.put(null, singletonMap(null, emptyMap()));

        // Structure = BTC, Funding = JPY
        DEFAULTS.get(CurrencyType.BTC).get(CurrencyType.JPY).put("bitflyer", "BTC_JPY");
        DEFAULTS.get(CurrencyType.BTC).get(CurrencyType.JPY).put("coincheck", "BTC_JPY");
        DEFAULTS.get(CurrencyType.BTC).get(CurrencyType.JPY).put("zaif", "btc_jpy");
        DEFAULTS.get(CurrencyType.BTC).get(CurrencyType.JPY).put("bitmex", "BXBTJPY");

        // Structure = BTC, Funding = USD
        DEFAULTS.get(CurrencyType.BTC).get(CurrencyType.USD).put("bitfinex", "btcusd");
        DEFAULTS.get(CurrencyType.BTC).get(CurrencyType.USD).put("poloniex", "USDT_BTC");
        DEFAULTS.get(CurrencyType.BTC).get(CurrencyType.USD).put("bitmex", "BXBT");

        // Structure = ETH, Funding = BTC
        DEFAULTS.get(CurrencyType.ETH).get(CurrencyType.BTC).put("bitflyer", "ETH_BTC");
        DEFAULTS.get(CurrencyType.ETH).get(CurrencyType.BTC).put("zaif", "eth_btc");
        DEFAULTS.get(CurrencyType.ETH).get(CurrencyType.BTC).put("bitfinex", "ethbtc");
        DEFAULTS.get(CurrencyType.ETH).get(CurrencyType.BTC).put("poloniex", "BTC_ETH");
        DEFAULTS.get(CurrencyType.ETH).get(CurrencyType.BTC).put("bitmex", "ETHXBT");

        // Structure = ETC, Funding = BTC
        DEFAULTS.get(CurrencyType.ETC).get(CurrencyType.BTC).put("bitmex", "ETCXBT");

        // Structure = BCT, Funding = BTC
        DEFAULTS.get(CurrencyType.BCH).get(CurrencyType.BTC).put("bitflyer", "BCH_BTC");
        DEFAULTS.get(CurrencyType.BCH).get(CurrencyType.BTC).put("zaif", "bch_btc");
        DEFAULTS.get(CurrencyType.BCH).get(CurrencyType.BTC).put("bitfinex", "bchbtc");
        DEFAULTS.get(CurrencyType.BCH).get(CurrencyType.BTC).put("poloniex", "BTC_BCH");
        DEFAULTS.get(CurrencyType.BCH).get(CurrencyType.BTC).put("bitmex", "BCHXBT");

        // Structure = USD, Funding = JPY
        DEFAULTS.get(CurrencyType.USD).get(CurrencyType.JPY).put("oanda", "USD_JPY");

    }

    protected Key getKey(Context context, Request request) {
        return Key.from(request);
    }

    protected Key convertKey(Context context, Request request, String site) {

        Key key = Key.from(request);

        CurrencyType structure = context.getInstrumentCurrency(key);

        CurrencyType funding = context.getFundingCurrency(key);

        String instrument = DEFAULTS.get(structure).get(funding).get(site);

        return Key.build(key).site(site).instrument(instrument).build();

    }

}
