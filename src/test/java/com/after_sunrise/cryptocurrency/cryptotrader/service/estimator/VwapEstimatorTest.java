package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.math.NumberUtils.LONG_ONE;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class VwapEstimatorTest {

    private VwapEstimator target;

    private Context context;

    @BeforeMethod
    public void setUp() throws Exception {

        context = mock(Context.class);

        target = spy(new VwapEstimator());

    }

    @Test
    public void testGet() throws Exception {
        assertEquals(target.get(), target.getClass().getSimpleName());
    }


    @Test
    public void testEstimate() throws Exception {

        Instant now = Instant.now();
        Instant from = now.minus(LONG_ONE, DAYS);
        Request request = Request.builder().currentTime(now).build();
        Key key = Key.from(request);

        Trade t1 = mock(Trade.class);
        Trade t2 = mock(Trade.class);
        Trade t3 = mock(Trade.class);
        Trade t4 = mock(Trade.class);
        Trade t5 = mock(Trade.class);
        Trade t6 = mock(Trade.class);
        Trade t7 = mock(Trade.class);
        when(t1.getTimestamp()).thenReturn(Instant.ofEpochMilli(1));
        when(t2.getTimestamp()).thenReturn(Instant.ofEpochMilli(2));
        when(t3.getTimestamp()).thenReturn(Instant.ofEpochMilli(3));
        when(t4.getTimestamp()).thenReturn(Instant.ofEpochMilli(4));
        when(t5.getTimestamp()).thenReturn(Instant.ofEpochMilli(5));
        when(t6.getTimestamp()).thenReturn(Instant.ofEpochMilli(6));
        when(t7.getTimestamp()).thenReturn(Instant.ofEpochMilli(7));
        when(t1.getPrice()).thenReturn(new BigDecimal("54.20735492"));
        when(t2.getPrice()).thenReturn(new BigDecimal("46.21598752"));
        when(t3.getPrice()).thenReturn(new BigDecimal("52.06059243"));
        when(t4.getPrice()).thenReturn(new BigDecimal("48.56048342"));
        when(t5.getPrice()).thenReturn(null);
        when(t6.getPrice()).thenReturn(new BigDecimal("12.345678901"));
        when(t7.getPrice()).thenReturn(new BigDecimal("23.456789012"));
        when(t1.getSize()).thenReturn(new BigDecimal("1"));
        when(t2.getSize()).thenReturn(new BigDecimal("2"));
        when(t3.getSize()).thenReturn(new BigDecimal("3"));
        when(t4.getSize()).thenReturn(new BigDecimal("4"));
        when(t5.getSize()).thenReturn(new BigDecimal("5"));
        when(t6.getSize()).thenReturn(null);
        when(t7.getSize()).thenReturn(new BigDecimal("-7"));

        when(context.listTrades(key, from)).thenReturn(asList(t1, t3, t5, t7, null, t2, t4, t6));
        Estimation estimation = target.estimate(context, request);
        assertEquals(estimation.getPrice(), new BigDecimal("49.706304093000"));
        assertEquals(estimation.getConfidence(), new BigDecimal("0.573471869268"));

        // One
        when(context.listTrades(key, from)).thenReturn(asList(t3));
        estimation = target.estimate(context, request);
        assertEquals(estimation.getPrice(), new BigDecimal("52.06059243"));
        assertEquals(estimation.getConfidence().stripTrailingZeros(), ONE);

        // One
        when(context.listTrades(key, from)).thenReturn(asList(t1, t2));
        estimation = target.estimate(context, request);
        assertEquals(estimation.getPrice(), new BigDecimal("48.879776653333"));
        assertEquals(estimation.getConfidence().stripTrailingZeros(), ONE);

        // Zero
        when(context.listTrades(key, from)).thenReturn(emptyList());
        estimation = target.estimate(context, request);
        assertEquals(estimation.getPrice(), null);
        assertEquals(estimation.getConfidence(), ZERO);

        // Null
        when(context.listTrades(key, from)).thenReturn(null);
        estimation = target.estimate(context, request);
        assertEquals(estimation.getPrice(), null);
        assertEquals(estimation.getConfidence(), ZERO);

    }

}
