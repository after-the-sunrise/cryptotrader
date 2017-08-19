package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import org.testng.annotations.Test;

import java.time.Instant;

import static java.math.BigDecimal.ZERO;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class TraderTest {

    @Test
    public void testRequest() throws Exception {

        assertFalse(Request.isValid(null));
        assertTrue(Request.isInvalid(null));

        Request.RequestBuilder builder = Request.builder();
        assertFalse(Request.isValid(builder.build()));
        assertTrue(Request.isInvalid(builder.build()));

        builder = builder.site("test");
        assertFalse(Request.isValid(builder.build()));
        assertTrue(Request.isInvalid(builder.build()));

        builder = builder.instrument("i");
        assertFalse(Request.isValid(builder.build()));
        assertTrue(Request.isInvalid(builder.build()));

        builder = builder.targetTime(Instant.now());
        assertFalse(Request.isValid(builder.build()));
        assertTrue(Request.isInvalid(builder.build()));

        builder = builder.tradingSpread(ZERO);
        assertFalse(Request.isValid(builder.build()));
        assertTrue(Request.isInvalid(builder.build()));

        builder = builder.tradingExposure(ZERO);
        assertFalse(Request.isValid(builder.build()));
        assertTrue(Request.isInvalid(builder.build()));

        builder = builder.tradingSplit(ZERO);
        assertTrue(Request.isValid(builder.build()));
        assertFalse(Request.isInvalid(builder.build()));

    }

}
