package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexService.FundingType;
import com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexService.ProductType;
import com.after_sunrise.cryptocurrency.cryptotrader.service.composite.CompositeService;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.LastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MidEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.VwapEstimator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitmexServiceTest {

    private Context context;

    private Request request;

    private Context.Key key;

    @BeforeMethod
    public void setUp() {

        context = mock(Context.class);

        request = Request.builder().site("bitmex").instrument("ETH_QT")
                .currentTime(Instant.now()).build();

        key = Context.Key.builder().site("bitmex").instrument("ETHXBT")
                .timestamp(request.getCurrentTime()).build();

    }

    @Test
    public void testFundingType() {

        for (FundingType funding : FundingType.values()) {

            assertSame(FundingType.findById(funding.getId()), funding);
            assertNull(FundingType.findById("foo"));
            assertNull(FundingType.findById(null));

        }

    }

    @Test
    public void testProductType() {

        for (ProductType product : ProductType.values()) {

            assertSame(ProductType.findByName(product.name()), product);
            assertNull(ProductType.findByName("foo"));
            assertNull(ProductType.findByName(null));

        }

    }

    @Test
    public void testBitfinexLastEstimator() {

        BitmexService.BitmexLastEstimator target = new BitmexService.BitmexLastEstimator();

        assertEquals(target.get(), "BitmexLastEstimator");

        assertTrue(LastEstimator.class.isInstance(target));

        assertEquals(target.getKey(request), key);

    }

    @Test
    public void testBitmexMidEstimator() {

        BitmexService.BitmexMidEstimator target = new BitmexService.BitmexMidEstimator();

        assertEquals(target.get(), "BitmexMidEstimator");

        assertTrue(MidEstimator.class.isInstance(target));

        assertEquals(target.getKey(request), key);

    }

    @Test
    public void testBitmexVwapEstimator() {

        BitmexService.BitmexVwapEstimator target = new BitmexService.BitmexVwapEstimator();

        assertEquals(target.get(), "BitmexVwapEstimator");

        assertTrue(VwapEstimator.class.isInstance(target));

        assertEquals(target.getKey(request), key);

    }

    @Test
    public void testBitmexCompositeMidEstimator() {

        BitmexService.BitmexCompositeMidEstimator target = new BitmexService.BitmexCompositeMidEstimator();

        assertEquals(target.get(), "BitmexCompositeMidEstimator");

        assertTrue(CompositeService.CompositeMidEstimator.class.isInstance(target));

        assertEquals(target.getKey(request), key);

    }

    @Test
    public void testBitmexCompositeLastEstimator() {

        BitmexService.BitmexCompositeLastEstimator target = new BitmexService.BitmexCompositeLastEstimator();

        assertEquals(target.get(), "BitmexCompositeLastEstimator");

        assertTrue(CompositeService.CompositeLastEstimator.class.isInstance(target));

        assertEquals(target.getKey(request), key);

    }

}
