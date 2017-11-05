package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.impl.AbstractService;

import java.util.HashMap;
import java.util.Map;

import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptyMap;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public abstract class AbstractEstimator extends AbstractService implements Estimator {

    protected static final Estimation BAIL = Estimation.builder().confidence(ZERO).build();

    // Source Site -> Source Product -> Target Site -> Target Product
    private static final Map<String, Map<String, Map<String, String>>> MAPPING = new HashMap<>();

    private static void addMapping(String ss, String sp, String ts, String tp) {
        MAPPING.computeIfAbsent(
                ss, key -> new HashMap<>()
        ).computeIfAbsent(
                sp, key -> new HashMap<>()
        ).put(
                ts, tp
        );
    }

    static {

        // bitflyer -> bitflyer
        addMapping("bitflyer", "BTC_JPY", "bitflyer", "FX_BTC_JPY");
        addMapping("bitflyer", "FX_BTC_JPY", "bitflyer", "BTC_JPY");
        addMapping("bitflyer", "BTCJPY_MAT1WK", "bitflyer", "BTC_JPY");
        addMapping("bitflyer", "BTCJPY_MAT2WK", "bitflyer", "BTC_JPY");

        // bitflyer -> coincheck
        addMapping("bitflyer", "BTC_JPY", "coincheck", "btc_jpy");
        addMapping("bitflyer", "FX_BTC_JPY", "coincheck", "btc_jpy");
        addMapping("bitflyer", "BTCJPY_MAT1WK", "coincheck", "btc_jpy");
        addMapping("bitflyer", "BTCJPY_MAT2WK", "coincheck", "btc_jpy");

        // bitflyer -> zaif
        addMapping("bitflyer", "BTC_JPY", "zaif", "btc_jpy");
        addMapping("bitflyer", "FX_BTC_JPY", "zaif", "btc_jpy");
        addMapping("bitflyer", "BTCJPY_MAT1WK", "zaif", "btc_jpy");
        addMapping("bitflyer", "BTCJPY_MAT2WK", "zaif", "btc_jpy");
        addMapping("bitflyer", "ETH_BTC", "zaif", "eth_btc");
        addMapping("bitflyer", "BCH_BTC", "zaif", "bch_btc");

        // bitflyer -> bitfinex
        addMapping("bitflyer", "ETH_BTC", "bitfinex", "ethbtc");
        addMapping("bitflyer", "BCH_BTC", "bitfinex", "bchbtc");
        addMapping("bitflyer", "BTC_JPY", "bitfinex", "*bitfinex:btcusd|*oanda:USD_JPY");
        addMapping("bitflyer", "FX_BTC_JPY", "bitfinex", "*bitfinex:btcusd|*oanda:USD_JPY");

        // bitflyer -> poloniex
        addMapping("bitflyer", "ETH_BTC", "poloniex", "BTC_ETH");
        addMapping("bitflyer", "BCH_BTC", "poloniex", "BTC_BCH");
        addMapping("bitflyer", "BTC_JPY", "poloniex", "*poloniex:USDT_BTC|*oanda:USD_JPY");
        addMapping("bitflyer", "FX_BTC_JPY", "poloniex", "*poloniex:USDT_BTC|*oanda:USD_JPY");

        // BitMEX -> BitMEX
        addMapping("bitmex", "ETH_QT", "bitmex", "ETHXBT");
        addMapping("bitmex", "XBTUSD", "bitmex", "BXBT");
        addMapping("bitmex", "XBT_QT", "bitmex", "*bitmex:XBJ_QT|/bitmex:BXBTJPY|*bitmex:BXBT");
        addMapping("bitmex", "XBJ_QT", "bitmex", "*bitmex:XBT_QT|*bitmex:BXBTJPY|/bitmex:BXBT");

    }

    protected Key getKey(Request request) {
        return Key.from(request);
    }

    protected Key convertKey(Request request, String site) {

        Key key = Key.from(request);

        String ss = key.getSite();

        String sp = key.getInstrument();

        String ts = site;

        String tp = MAPPING.getOrDefault(ss, emptyMap()).getOrDefault(sp, emptyMap()).get(ts);

        return Key.build(key).site(ts).instrument(tp).build();

    }

}
