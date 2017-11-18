package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType;
import com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.AssetType;
import com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ProductType;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.LastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MicroEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MidEstimator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ProductType.BTC_JPY;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ProductType.ETH_BTC;
import static java.math.RoundingMode.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitflyerServiceTest {

    private Context context;

    private Request request;

    private Context.Key key;

    @BeforeMethod
    public void setUp() {

        context = mock(Context.class);

        request = Request.builder().site("s").instrument("i").currentTime(Instant.now()).build();
        when(context.getInstrumentCurrency(Context.Key.from(request))).thenReturn(CurrencyType.BTC);
        when(context.getFundingCurrency(Context.Key.from(request))).thenReturn(CurrencyType.JPY);

        key = Context.Key.builder().site("bitflyer").instrument("BTC_JPY")
                .timestamp(request.getCurrentTime()).build();

    }

    @Test
    public void testBitflyerLastEstimator() {

        BitflyerService.BitflyerLastEstimator target = new BitflyerService.BitflyerLastEstimator();

        assertEquals(target.get(), "BitflyerLastEstimator");

        assertTrue(LastEstimator.class.isInstance(target));

        assertEquals(target.getKey(context, request), key);

    }

    @Test
    public void testBitflyerMicroEstimator() {

        BitflyerService.BitflyerMicroEstimator target = new BitflyerService.BitflyerMicroEstimator();

        assertEquals(target.get(), "BitflyerMicroEstimator");

        assertTrue(MicroEstimator.class.isInstance(target));

        assertEquals(target.getKey(context, request), key);

    }

    @Test
    public void testBitflyerMidEstimator() {

        BitflyerService.BitflyerMidEstimator target = new BitflyerService.BitflyerMidEstimator();

        assertEquals(target.get(), "BitflyerMidEstimator");

        assertTrue(MidEstimator.class.isInstance(target));

        assertEquals(target.getKey(context, request), key);

    }

    @Test
    public void testAssetType() {

        for (AssetType target : AssetType.values()) {
            assertSame(AssetType.find(target.name()), target);
            assertNull(AssetType.find("hoge"));
            assertNull(AssetType.find(""));
            assertNull(AssetType.find(null));

            assertNotNull(target.getCurrency());
        }

    }

    @Test
    public void testProductType() {

        for (ProductType target : ProductType.values()) {
            assertSame(ProductType.find(target.name()), target);
            assertNull(ProductType.find("hoge"));
            assertNull(ProductType.find(""));
            assertNull(ProductType.find(null));

            assertNotNull(target.getFunding());
            assertNotNull(target.getStructure());
            assertNotNull(target.getLotSize());
            assertNotNull(target.getTickSize());
        }

    }

    @Test
    public void testProductType_Underlying() throws Exception {

        for (ProductType type : ProductType.values()) {

            ProductType expect = null;

            switch (type) {
                case BTCJPY_MAT1WK:
                case BTCJPY_MAT2WK:
                    expect = BTC_JPY;
                    break;
            }

            String message = String.format("%s -> %s", type, expect);

            assertEquals(type.getUnderlying(), expect, message);

        }

    }

    @Test
    public void testProductType_RoundToLotSize() throws Exception {

        BigDecimal value = new BigDecimal("0.0015");

        assertEquals(BTC_JPY.roundToLotSize(value, UP), new BigDecimal("0.002"));
        assertEquals(BTC_JPY.roundToLotSize(value, HALF_UP), new BigDecimal("0.002"));

        assertEquals(BTC_JPY.roundToLotSize(value, DOWN), new BigDecimal("0.001"));
        assertEquals(BTC_JPY.roundToLotSize(value, HALF_DOWN), new BigDecimal("0.001"));

        assertNull(BTC_JPY.roundToLotSize(null, DOWN));
        assertNull(BTC_JPY.roundToLotSize(value, null));
        assertNull(BTC_JPY.roundToLotSize(null, null));

    }

    @Test
    public void testProductType_testRoundToTickSize() throws Exception {

        BigDecimal value = new BigDecimal("0.000015");

        assertEquals(ETH_BTC.roundToTickSize(value, UP), new BigDecimal("0.00002"));
        assertEquals(ETH_BTC.roundToTickSize(value, HALF_UP), new BigDecimal("0.00002"));

        assertEquals(ETH_BTC.roundToTickSize(value, DOWN), new BigDecimal("0.00001"));
        assertEquals(ETH_BTC.roundToTickSize(value, HALF_DOWN), new BigDecimal("0.00001"));

        assertNull(ETH_BTC.roundToTickSize(null, DOWN));
        assertNull(ETH_BTC.roundToTickSize(value, null));
        assertNull(ETH_BTC.roundToTickSize(null, null));

    }

}
