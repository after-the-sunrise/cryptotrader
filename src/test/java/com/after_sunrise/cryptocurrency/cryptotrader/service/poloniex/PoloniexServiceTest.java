package com.after_sunrise.cryptocurrency.cryptotrader.service.poloniex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.LastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MicroEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MidEstimator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.BTC;
import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.ETH;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class PoloniexServiceTest {

    private Context context;

    private Request request;

    private Key key;

    @BeforeMethod
    public void setUp() {

        context = mock(Context.class);

        request = Request.builder().site("s").instrument("i").currentTime(Instant.now()).build();
        when(context.getInstrumentCurrency(Key.from(request))).thenReturn(ETH);
        when(context.getFundingCurrency(Key.from(request))).thenReturn(BTC);
        when(context.findProduct(Key.builder().site("poloniex").instrument("*")
                .timestamp(request.getCurrentTime()).build(), ETH, BTC)).thenReturn("BTC_ETH");

        key = Key.builder().site("poloniex").instrument("BTC_ETH")
                .timestamp(request.getCurrentTime()).build();

    }

    @Test
    public void testPoloniexLastEstimator() {

        PoloniexService.PoloniexLastEstimator target = new PoloniexService.PoloniexLastEstimator();

        assertEquals(target.get(), "PoloniexLastEstimator");

        assertTrue(LastEstimator.class.isInstance(target));

        assertEquals(target.getKey(context, request), key);

    }

    @Test
    public void testPoloniexMicroEstimator() {

        PoloniexService.PoloniexMicroEstimator target = new PoloniexService.PoloniexMicroEstimator();

        assertEquals(target.get(), "PoloniexMicroEstimator");

        assertTrue(MicroEstimator.class.isInstance(target));

        assertEquals(target.getKey(context, request), key);

    }

    @Test
    public void testPoloniexMidEstimator() {

        PoloniexService.PoloniexMidEstimator target = new PoloniexService.PoloniexMidEstimator();

        assertEquals(target.get(), "PoloniexMidEstimator");

        assertTrue(MidEstimator.class.isInstance(target));

        assertEquals(target.getKey(context, request), key);

    }

}
