package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType;
import com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexService.FundingType;
import com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexService.ProductType;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.LastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MidEstimator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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

        request = Request.builder().site("s").instrument("i").currentTime(Instant.now()).build();
        when(context.getInstrumentCurrency(Context.Key.from(request))).thenReturn(CurrencyType.ETH);
        when(context.getFundingCurrency(Context.Key.from(request))).thenReturn(CurrencyType.BTC);

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

        assertEquals(target.getKey(context, request), key);

    }

    @Test
    public void testBitmexMidEstimator() {

        BitmexService.BitmexMidEstimator target = new BitmexService.BitmexMidEstimator();

        assertEquals(target.get(), "BitmexMidEstimator");

        assertTrue(MidEstimator.class.isInstance(target));

        assertEquals(target.getKey(context, request), key);

    }

}
