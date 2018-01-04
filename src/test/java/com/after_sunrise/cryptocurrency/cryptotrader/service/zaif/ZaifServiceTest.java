package com.after_sunrise.cryptocurrency.cryptotrader.service.zaif;

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
import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.JPY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class ZaifServiceTest {

    private Context context;

    private Request request;

    private Key key;

    @BeforeMethod
    public void setUp() {

        context = mock(Context.class);

        request = Request.builder().site("s").instrument("i").currentTime(Instant.now()).build();
        when(context.getInstrumentCurrency(Key.from(request))).thenReturn(BTC);
        when(context.getFundingCurrency(Key.from(request))).thenReturn(JPY);
        when(context.findProduct(Key.builder().site("zaif").instrument("*")
                .timestamp(request.getCurrentTime()).build(), BTC, JPY)).thenReturn("btc_jpy");

        key = Key.builder().site("zaif").instrument("btc_jpy")
                .timestamp(request.getCurrentTime()).build();

    }

    @Test
    public void testZaifLastEstimator() {

        ZaifService.ZaifLastEstimator target = new ZaifService.ZaifLastEstimator();

        assertEquals(target.get(), "ZaifLastEstimator");

        assertTrue(LastEstimator.class.isInstance(target));

        assertEquals(target.getKey(context, request), key);

    }

    @Test
    public void testZaifMicroEstimator() {

        ZaifService.ZaifMicroEstimator target = new ZaifService.ZaifMicroEstimator();

        assertEquals(target.get(), "ZaifMicroEstimator");

        assertTrue(MicroEstimator.class.isInstance(target));

        assertEquals(target.getKey(context, request), key);

    }

    @Test
    public void testZaifMidEstimator() {

        ZaifService.ZaifMidEstimator target = new ZaifService.ZaifMidEstimator();

        assertEquals(target.get(), "ZaifMidEstimator");

        assertTrue(MidEstimator.class.isInstance(target));

        assertEquals(target.getKey(context, request), key);

    }

}
