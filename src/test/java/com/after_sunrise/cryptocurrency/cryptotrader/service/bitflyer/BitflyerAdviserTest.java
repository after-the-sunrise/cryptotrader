package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.math.BigDecimal.ZERO;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitflyerAdviserTest {

    private BitflyerAdviser target;

    private TestModule module;

    private Context context;

    @BeforeMethod
    public void setUp() throws Exception {

        module = new TestModule();

        context = module.getMock(Context.class);

        target = new BitflyerAdviser();

    }

    @Test
    public void testGet() {
        assertEquals(target.get(), BitflyerService.ID);
    }


    @Test
    public void testCalculateBasis() {

        ZoneId zone = ZoneId.of("Asia/Tokyo");
        LocalDateTime zdt = LocalDateTime.of(2017, 8, 25, 8, 0);
        Instant now = ZonedDateTime.of(zdt, zone).toInstant();

        Request.RequestBuilder b = Request.builder().tradingSpread(new BigDecimal("0.0020")).currentTime(now);
        when(context.getCommissionRate(Key.from(b.build()))).thenReturn(new BigDecimal("0.0010"));

        // SD (No Swap)
        LocalDateTime exp = LocalDateTime.of(2017, 8, 25, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateBasis(context, b.build()), new BigDecimal("0.0030"));

        // Past (No Swap)
        exp = LocalDateTime.of(2017, 8, 24, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateBasis(context, b.build()), new BigDecimal("0.0030"));

        // S+1
        exp = LocalDateTime.of(2017, 8, 26, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateBasis(context, b.build()), new BigDecimal("0.0034000000"));

        // S+7
        exp = LocalDateTime.of(2017, 9, 1, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateBasis(context, b.build()), new BigDecimal("0.0058033623"));

        // S+14
        exp = LocalDateTime.of(2017, 9, 8, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateBasis(context, b.build()), new BigDecimal("0.0086145834"));

        // Null base
        assertNull(target.calculateBasis(context, Request.builder().currentTime(now).build()));

        // Null current time
        assertNull(target.calculateBasis(context, Request.builder().tradingSpread(ZERO).build()));

        // Null Expiry
        when(context.getExpiry(Key.from(b.build()))).thenReturn(null);
        assertEquals(target.calculateBasis(context, b.build()), new BigDecimal("0.0030"));

    }

}
