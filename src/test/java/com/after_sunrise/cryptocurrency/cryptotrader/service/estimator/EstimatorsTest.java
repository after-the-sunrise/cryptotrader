package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import org.testng.annotations.Test;

import java.time.Instant;

import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class EstimatorsTest {

    @Test
    public void testGetKey() throws Exception {

        Instant now = Instant.now();
        Request.RequestBuilder b = Request.builder().currentTime(now);

        Context.Key key = Estimators.getKey(b.build(), null);
        assertEquals(key.getSite(), null);
        assertEquals(key.getInstrument(), null);
        assertEquals(key.getTimestamp(), now);

        key = Estimators.getKey(b.build(), "test");
        assertEquals(key.getSite(), "test");
        assertEquals(key.getInstrument(), null);
        assertEquals(key.getTimestamp(), now);

        key = Estimators.getKey(b.site("bitflyer").build(), null);
        assertEquals(key.getSite(), null);
        assertEquals(key.getInstrument(), null);
        assertEquals(key.getTimestamp(), now);

        key = Estimators.getKey(b.site("bitflyer").instrument("BTC_JPY").build(), null);
        assertEquals(key.getSite(), null);
        assertEquals(key.getInstrument(), null);
        assertEquals(key.getTimestamp(), now);

        key = Estimators.getKey(b.site("bitflyer").instrument("BTC_JPY").build(), "coincheck");
        assertEquals(key.getSite(), "coincheck");
        assertEquals(key.getInstrument(), "btc_jpy");
        assertEquals(key.getTimestamp(), now);

    }

}
