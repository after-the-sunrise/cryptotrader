package com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck;

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
public class CoincheckServiceTest {

    private Context context;

    private Request request;

    private Key key;

    @BeforeMethod
    public void setUp() {

        context = mock(Context.class);

        request = Request.builder().site("bitflyer").instrument("BTC_JPY")
                .currentTime(Instant.now()).build();

        key = Key.builder().site("coincheck").instrument("btc_jpy")
                .timestamp(request.getCurrentTime()).build();

    }

    @Test
    public void testCoincheckLastEstimator() {

        CoincheckService.CoincheckLastEstimator target = new CoincheckService.CoincheckLastEstimator();

        assertEquals(target.get(), "CoincheckLastEstimator");

        assertTrue(LastEstimator.class.isInstance(target));

        assertEquals(target.getKey(request), key);

    }

    @Test
    public void testCoincheckMidEstimator() {

        CoincheckService.CoincheckMidEstimator target = new CoincheckService.CoincheckMidEstimator();

        assertEquals(target.get(), "CoincheckMidEstimator");

        assertTrue(MidEstimator.class.isInstance(target));

        assertEquals(target.getKey(request), key);

    }

    @Test
    public void testCoincheckVwapEstimator() {

        CoincheckService.CoincheckVwapEstimator target = new CoincheckService.CoincheckVwapEstimator();

        assertEquals(target.get(), "CoincheckVwapEstimator");

        assertTrue(VwapEstimator.class.isInstance(target));

        assertEquals(target.getKey(request), key);

    }

}
