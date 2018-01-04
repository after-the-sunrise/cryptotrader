package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.BTC;
import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.JPY;
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
        Context.Key newKey = Context.Key.build(key).site("x").instrument("*").build();

        when(context.getInstrumentCurrency(key)).thenReturn(BTC);
        when(context.getFundingCurrency(key)).thenReturn(JPY);
        when(context.findProduct(newKey, BTC, JPY)).thenReturn("JPY/BTC");

        Context.Key result = target.convertKey(context, request, "x");
        assertEquals(result.getSite(), "x");
        assertEquals(result.getInstrument(), "JPY/BTC");
        assertEquals(result.getTimestamp(), now);

    }

}
