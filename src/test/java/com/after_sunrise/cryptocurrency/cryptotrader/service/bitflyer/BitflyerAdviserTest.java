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
import static org.mockito.Mockito.*;
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

        target = spy(new BitflyerAdviser());

    }

    @Test
    public void testGet() {
        assertEquals(target.get(), BitflyerService.ID);
    }

    @Test
    public void testCalculateSwapRate() {

        ZoneId zone = ZoneId.of("Asia/Tokyo");
        LocalDateTime zdt = LocalDateTime.of(2017, 8, 25, 8, 0);
        Instant now = ZonedDateTime.of(zdt, zone).toInstant();

        Request.RequestBuilder b = Request.builder().currentTime(now);

        // SD (No Swap)
        LocalDateTime exp = LocalDateTime.of(2017, 8, 25, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateSwapRate(context, b.build()), ZERO);

        // Past (No Swap)
        exp = LocalDateTime.of(2017, 8, 24, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateSwapRate(context, b.build()), ZERO);

        // S+1
        exp = LocalDateTime.of(2017, 8, 26, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateSwapRate(context, b.build()), new BigDecimal("0.0004000000"));

        // S+7
        exp = LocalDateTime.of(2017, 9, 1, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateSwapRate(context, b.build()), new BigDecimal("0.0028033623"));

        // S+14
        exp = LocalDateTime.of(2017, 9, 8, 16, 0);
        when(context.getExpiry(Key.from(b.build()))).thenReturn(ZonedDateTime.of(exp, zone));
        assertEquals(target.calculateSwapRate(context, b.build()), new BigDecimal("0.0056145834"));

        // Null current time
        assertEquals(target.calculateSwapRate(context, Request.builder().build()), ZERO);

        // Null Expiry
        when(context.getExpiry(Key.from(b.build()))).thenReturn(null);
        assertEquals(target.calculateSwapRate(context, b.build()), ZERO);

    }

    @Test
    public void testAdjustBasis() {

        Request request = Request.builder().build();

        doReturn(new BigDecimal("0.0005")).when(target).calculateSwapRate(context, request);

        BigDecimal result = target.adjustBasis(context, request, new BigDecimal("0.002"));

        assertEquals(result, new BigDecimal("0.0025"));

        assertNull(target.adjustBasis(context, request, null));

    }

}
