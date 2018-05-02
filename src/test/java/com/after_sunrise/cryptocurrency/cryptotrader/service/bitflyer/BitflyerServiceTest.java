package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.AssetType;
import com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ProductType;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ProductType.BTC_JPY;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ProductType.ETH_BTC;
import static java.math.RoundingMode.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitflyerServiceTest {

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
                case BTCJPY_MAT3M:
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
