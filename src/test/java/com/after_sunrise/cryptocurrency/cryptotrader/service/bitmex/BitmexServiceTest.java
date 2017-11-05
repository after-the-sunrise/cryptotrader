package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

import com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexService.FundingType;
import com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexService.ProductType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitmexServiceTest {

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

}
