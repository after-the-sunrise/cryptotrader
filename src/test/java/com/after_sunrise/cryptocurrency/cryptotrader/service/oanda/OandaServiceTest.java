package com.after_sunrise.cryptocurrency.cryptotrader.service.oanda;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.LastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MidEstimator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.JPY;
import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.USD;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class OandaServiceTest {

    private Context context;

    private Request request;

    private Key key;

    @BeforeMethod
    public void setUp() {

        context = mock(Context.class);

        request = Request.builder().site("s").instrument("i").currentTime(Instant.now()).build();
        when(context.getInstrumentCurrency(Key.from(request))).thenReturn(USD);
        when(context.getFundingCurrency(Key.from(request))).thenReturn(JPY);
        when(context.findProduct(Key.builder().site("oanda").instrument("*")
                .timestamp(request.getCurrentTime()).build(), USD, JPY)).thenReturn("USD_JPY");

        key = Key.builder().site("oanda").instrument("USD_JPY")
                .timestamp(request.getCurrentTime()).build();

    }

    @Test
    public void testOandaLastEstimator() {

        OandaService.OandaLastEstimator target = new OandaService.OandaLastEstimator();

        assertEquals(target.get(), "OandaLastEstimator");

        assertTrue(LastEstimator.class.isInstance(target));

        assertEquals(target.getKey(context, request), key);

    }

    @Test
    public void testOandaMidEstimator() {

        OandaService.OandaMidEstimator target = new OandaService.OandaMidEstimator();

        assertEquals(target.get(), "OandaMidEstimator");

        assertTrue(MidEstimator.class.isInstance(target));

        assertEquals(target.getKey(context, request), key);

    }

}
