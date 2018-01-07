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

import static java.math.BigDecimal.ZERO;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.math.NumberUtils.LONG_ONE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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

        target = new VwapEstimator();

    }

    @Test
    public void testGet() throws Exception {
        assertEquals(target.get(), target.getClass().getSimpleName());
    }

    @Test
    public void testEstimate() throws Exception {

        Instant now = Instant.now();
        Instant from = now.minus(LONG_ONE, HOURS);
        Request request = Request.builder().currentTime(now).build();
        Key key = Key.from(request);

        Trade t1 = mock(Trade.class); // Valid
        Trade t2 = mock(Trade.class); // Valid
        Trade t3 = mock(Trade.class); // Null Timestamp
        Trade t4 = mock(Trade.class); // Null Price
        Trade t5 = mock(Trade.class); // Null Size
        Trade t6 = mock(Trade.class); // Negative Size
        Trade t7 = mock(Trade.class); // Valid
        Trade t8 = mock(Trade.class); // Valid
        when(t1.getTimestamp()).thenReturn(from.plus(10, MINUTES));
        when(t2.getTimestamp()).thenReturn(from.plus(20, MINUTES));
        when(t3.getTimestamp()).thenReturn(null);
        when(t4.getTimestamp()).thenReturn(from.plus(30, MINUTES));
        when(t5.getTimestamp()).thenReturn(from.plus(40, MINUTES));
        when(t6.getTimestamp()).thenReturn(from.plus(50, MINUTES));
        when(t7.getTimestamp()).thenReturn(from.plus(60, MINUTES));
        when(t8.getTimestamp()).thenReturn(from.plus(70, MINUTES));
        when(t1.getPrice()).thenReturn(new BigDecimal("54.20735492"));
        when(t2.getPrice()).thenReturn(new BigDecimal("46.21598752"));
        when(t3.getPrice()).thenReturn(new BigDecimal("34.56789012"));
        when(t4.getPrice()).thenReturn(null);
        when(t5.getPrice()).thenReturn(new BigDecimal("56.78901234"));
        when(t6.getPrice()).thenReturn(new BigDecimal("67.89012345"));
        when(t7.getPrice()).thenReturn(new BigDecimal("52.06059242"));
        when(t8.getPrice()).thenReturn(new BigDecimal("48.56048341"));
        when(t1.getSize()).thenReturn(new BigDecimal("+1"));
        when(t2.getSize()).thenReturn(new BigDecimal("+2"));
        when(t3.getSize()).thenReturn(new BigDecimal("+3"));
        when(t4.getSize()).thenReturn(new BigDecimal("+4"));
        when(t5.getSize()).thenReturn(new BigDecimal("-5"));
        when(t6.getSize()).thenReturn(null);
        when(t7.getSize()).thenReturn(new BigDecimal("+3"));
        when(t8.getSize()).thenReturn(new BigDecimal("+4"));

        // All ticks
        when(context.listTrades(key, from)).thenReturn(asList(t1, t3, t5, t7, null, t2, t4, t6, t8));
        Estimation estimation = target.estimate(context, request);
        assertEquals(estimation.getPrice(), new BigDecimal("49.9231910345"));
        assertEquals(estimation.getConfidence(), new BigDecimal("0.9388333118"));

        // Two points
        when(context.listTrades(key, from)).thenReturn(asList(t1, null, t2));
        estimation = target.estimate(context, request);
        assertEquals(estimation.getPrice(), new BigDecimal("48.8298179723"));
        assertEquals(estimation.getConfidence().signum(), 0);

        // One point
        when(context.listTrades(key, from)).thenReturn(asList(null, t1));
        estimation = target.estimate(context, request);
        assertEquals(estimation.getPrice(), t1.getPrice());
        assertEquals(estimation.getConfidence(), ZERO);

        // Zero point
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
