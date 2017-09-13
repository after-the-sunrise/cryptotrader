package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class Estimators {

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
        addMapping("bitflyer", "BTC_JPY", "coincheck", "btc_jpy");
        addMapping("bitflyer", "BTC_JPY", "zaif", "BTC/JPY");
        addMapping("bitflyer", "BCH_BTC", "poloniex", "BTC_BCH");
        addMapping("bitflyer", "ETH_BTC", "poloniex", "BTC_ETH");
        addMapping("bitflyer", "BCH_BTC", "bitfinex", "bchbtc");
        addMapping("bitflyer", "ETH_BTC", "bitfinex", "ethbtc");
    }

    public static Key getKey(Request request, String site) {

        Key key = Key.from(request);

        String ss = key.getSite();

        String sp = key.getInstrument();

        String ts = site;

        String tp = MAPPING.getOrDefault(ss, emptyMap()).getOrDefault(sp, emptyMap()).get(ts);

        return Key.build(key).site(ts).instrument(tp).build();

    }

}
