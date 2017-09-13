package com.after_sunrise.cryptocurrency.cryptotrader.service.poloniex;

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
public class PoloniexServiceTest {

    private Context context;

    private Request request;

    private Key key;

    @BeforeMethod
    public void setUp() {

        context = mock(Context.class);

        request = Request.builder().site("bitflyer").instrument("ETH_BTC")
                .currentTime(Instant.now()).build();

        key = Key.builder().site("poloniex").instrument("BTC_ETH")
                .timestamp(request.getCurrentTime()).build();

    }

    @Test
    public void testPoloniexLastEstimator() {

        PoloniexService.PoloniexLastEstimator target = new PoloniexService.PoloniexLastEstimator();

        assertEquals(target.get(), "PoloniexLastEstimator");

        assertTrue(LastEstimator.class.isInstance(target));

        assertEquals(target.getKey(request), key);

    }

    @Test
    public void testPoloniexMidEstimator() {

        PoloniexService.PoloniexMidEstimator target = new PoloniexService.PoloniexMidEstimator();

        assertEquals(target.get(), "PoloniexMidEstimator");

        assertTrue(MidEstimator.class.isInstance(target));

        assertEquals(target.getKey(request), key);

    }

    @Test
    public void testPoloniexVwapEstimator() {

        PoloniexService.PoloniexVwapEstimator target = new PoloniexService.PoloniexVwapEstimator();

        assertEquals(target.get(), "PoloniexVwapEstimator");

        assertTrue(VwapEstimator.class.isInstance(target));

        assertEquals(target.getKey(request), key);

    }

}
