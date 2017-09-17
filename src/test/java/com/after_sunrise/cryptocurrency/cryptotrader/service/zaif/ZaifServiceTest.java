package com.after_sunrise.cryptocurrency.cryptotrader.service.zaif;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.LastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MidEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.VwapEstimator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;

import static org.mockito.Mockito.mock;
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

        request = Request.builder().site("bitflyer").instrument("BTC_JPY")
                .currentTime(Instant.now()).build();

        key = Key.builder().site("zaif").instrument("btc_jpy")
                .timestamp(request.getCurrentTime()).build();

    }

    @Test
    public void testZaifLastEstimator() {

        ZaifService.ZaifLastEstimator target = new ZaifService.ZaifLastEstimator();

        assertEquals(target.get(), "ZaifLastEstimator");

        assertTrue(LastEstimator.class.isInstance(target));

        assertEquals(target.getKey(request), key);

    }

    @Test
    public void testZaifMidEstimator() {

        ZaifService.ZaifMidEstimator target = new ZaifService.ZaifMidEstimator();

        assertEquals(target.get(), "ZaifMidEstimator");

        assertTrue(MidEstimator.class.isInstance(target));

        assertEquals(target.getKey(request), key);

    }

    @Test
    public void testZaifVwapEstimator() {

        ZaifService.ZaifVwapEstimator target = new ZaifService.ZaifVwapEstimator();

        assertEquals(target.get(), "ZaifVwapEstimator");

        assertTrue(VwapEstimator.class.isInstance(target));

        assertEquals(target.getKey(request), key);

    }

}
