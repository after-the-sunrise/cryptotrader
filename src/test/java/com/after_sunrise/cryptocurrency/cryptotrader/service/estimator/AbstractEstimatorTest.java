package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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

    private Context context;

    @BeforeMethod
    public void setUp() throws Exception {

        context = mock(Context.class);

        target = new TestEstimator();

    }

    @Test
    public void testGetKey() throws Exception {

        Request r = Request.builder().build();

        assertEquals(target.getKey(null, r), Context.Key.from(r));

    }

    @Test
    public void testConvertKey() throws Exception {

        Instant now = Instant.now();
        Request request = Request.builder().site("s").instrument("i").currentTime(now).build();
        Context.Key key = Context.Key.from(request);

        when(context.getInstrumentCurrency(key)).thenReturn(BTC, ETH, BCH, USD, null);
        when(context.getFundingCurrency(key)).thenReturn(JPY, BTC, BTC, JPY, null);

        // BTC JPY
        key = target.convertKey(context, request, "bitflyer");
        assertEquals(key.getSite(), "bitflyer");
        assertEquals(key.getInstrument(), "BTC_JPY");
        assertEquals(key.getTimestamp(), now);

        // ETH BTC
        key = target.convertKey(context, request, "bitfinex");
        assertEquals(key.getSite(), "bitfinex");
        assertEquals(key.getInstrument(), "ethbtc");
        assertEquals(key.getTimestamp(), now);

        // BCH BTC
        key = target.convertKey(context, request, "poloniex");
        assertEquals(key.getSite(), "poloniex");
        assertEquals(key.getInstrument(), "BTC_BCH");
        assertEquals(key.getTimestamp(), now);

        // USD JPY
        key = target.convertKey(context, request, "oanda");
        assertEquals(key.getSite(), "oanda");
        assertEquals(key.getInstrument(), "USD_JPY");
        assertEquals(key.getTimestamp(), now);

        // None
        key = target.convertKey(context, request, "foo");
        assertEquals(key.getSite(), "foo");
        assertEquals(key.getInstrument(), null);
        assertEquals(key.getTimestamp(), now);

    }

}
