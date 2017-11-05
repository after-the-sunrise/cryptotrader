package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;

import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class AbstractEstimatorTest {

    static class TestEstimator extends AbstractEstimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return BAIL;
        }
    }

    private AbstractEstimator target;

    @BeforeMethod
    public void setUp() throws Exception {
        target = new TestEstimator();
    }

    @Test
    public void testGetKey() throws Exception {

        Request r = Request.builder().build();

        assertEquals(target.getKey(r), Context.Key.from(r));

    }

    @Test
    public void testConvertKey() throws Exception {

        Instant now = Instant.now();
        Request.RequestBuilder b = Request.builder().currentTime(now);

        Context.Key key = target.convertKey(b.build(), null);
        assertEquals(key.getSite(), null);
        assertEquals(key.getInstrument(), null);
        assertEquals(key.getTimestamp(), now);

        key = target.convertKey(b.build(), "test");
        assertEquals(key.getSite(), "test");
        assertEquals(key.getInstrument(), null);
        assertEquals(key.getTimestamp(), now);

        key = target.convertKey(b.site("bitflyer").build(), null);
        assertEquals(key.getSite(), null);
        assertEquals(key.getInstrument(), null);
        assertEquals(key.getTimestamp(), now);

        key = target.convertKey(b.site("bitflyer").instrument("BTC_JPY").build(), null);
        assertEquals(key.getSite(), null);
        assertEquals(key.getInstrument(), null);
        assertEquals(key.getTimestamp(), now);

        key = target.convertKey(b.site("bitflyer").instrument("BTC_JPY").build(), "coincheck");
        assertEquals(key.getSite(), "coincheck");
        assertEquals(key.getInstrument(), "btc_jpy");
        assertEquals(key.getTimestamp(), now);

    }

}
