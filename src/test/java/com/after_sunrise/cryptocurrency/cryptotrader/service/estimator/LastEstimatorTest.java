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
import java.util.List;

import static java.math.BigDecimal.*;
import static java.time.temporal.ChronoUnit.*;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.math.NumberUtils.LONG_ONE;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class LastEstimatorTest {

    private LastEstimator target;

    private Context context;

    @BeforeMethod
    public void setUp() throws Exception {

        context = mock(Context.class);

        target = spy(new LastEstimator());

    }

    @Test
    public void testGet() throws Exception {
        assertEquals(target.get(), target.getClass().getSimpleName());
    }

    @Test
    public void testEstimate() throws Exception {

        Instant time = Instant.now();
        doReturn(TEN).when(target).calculateConfidence(time.minusSeconds(1), time);

        Trade t1 = mock(Trade.class);
        Trade t2 = mock(Trade.class);
        Trade t3 = mock(Trade.class);
        Trade t4 = mock(Trade.class);
        Trade t5 = mock(Trade.class);
        when(t1.getTimestamp()).thenReturn(time.minusSeconds(1));
        when(t2.getTimestamp()).thenReturn(null);
        when(t3.getTimestamp()).thenReturn(time.minusSeconds(3));
        when(t4.getTimestamp()).thenReturn(time.minusSeconds(4));
        when(t5.getTimestamp()).thenReturn(time.minusSeconds(5));
        when(t1.getPrice()).thenReturn(BigDecimal.valueOf(3));
        when(t2.getPrice()).thenReturn(BigDecimal.valueOf(4));
        when(t3.getPrice()).thenReturn(BigDecimal.valueOf(5));
        when(t4.getPrice()).thenReturn(null);
        when(t5.getPrice()).thenReturn(BigDecimal.valueOf(2));

        Request request = Request.builder().site("s").instrument("i").currentTime(time).build();
        Key key = Key.from(request);
        List<Trade> trades = asList(t3, t2, null, t5, null, t1, t4);
        when(context.listTrades(key, time.minus(LONG_ONE, HOURS))).thenReturn(trades);

        // Estimated
        Estimation result = target.estimate(context, request);
        assertEquals(result.getPrice(), t1.getPrice());
        assertEquals(result.getConfidence(), TEN);

        // Null trades
        when(context.listTrades(key, time.minus(LONG_ONE, HOURS))).thenReturn(null);
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), ZERO);

        // Invalid trades
        when(context.listTrades(key, time.minus(LONG_ONE, HOURS))).thenReturn(asList((Trade) null));
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), ZERO);

    }

    @Test
    public void testCalculateConfidence() {

        Instant now = Instant.now();

        // No diff
        assertEquals(target.calculateConfidence(now, now), ONE);

        // Future date
        assertEquals(target.calculateConfidence(now.plusSeconds(1), now), ONE);

        // Seconds
        assertEquals(target.calculateConfidence(now.minusSeconds(01), now), new BigDecimal("0.8122277435"));
        assertEquals(target.calculateConfidence(now.minusSeconds(10), now), new BigDecimal("0.7496369913"));
        assertEquals(target.calculateConfidence(now.minusSeconds(20), now), new BigDecimal("0.7307952975"));
        assertEquals(target.calculateConfidence(now.minusSeconds(30), now), new BigDecimal("0.7197736131"));
        assertEquals(target.calculateConfidence(now.minusSeconds(40), now), new BigDecimal("0.7119536036"));
        assertEquals(target.calculateConfidence(now.minusSeconds(50), now), new BigDecimal("0.7058879330"));

        // Minutes
        assertEquals(target.calculateConfidence(now.minus(01, MINUTES), now), new BigDecimal("0.7009319193"));
        assertEquals(target.calculateConfidence(now.minus(05, MINUTES), now), new BigDecimal("0.6571828610"));
        assertEquals(target.calculateConfidence(now.minus(15, MINUTES), now), new BigDecimal("0.6273194827"));
        assertEquals(target.calculateConfidence(now.minus(30, MINUTES), now), new BigDecimal("0.6084777889"));
        assertEquals(target.calculateConfidence(now.minus(45, MINUTES), now), new BigDecimal("0.5974561045"));

        // Hours
        assertEquals(target.calculateConfidence(now.minus(1, HOURS), now), new BigDecimal("0.5896360950"));
        assertEquals(target.calculateConfidence(now.minus(4, HOURS), now), new BigDecimal("0.5519527073"));
        assertEquals(target.calculateConfidence(now.minus(8, HOURS), now), new BigDecimal("0.5331110135"));
        assertEquals(target.calculateConfidence(now.minus(16, HOURS), now), new BigDecimal("0.5142693196"));

        // Days
        assertEquals(target.calculateConfidence(now.minus(1, DAYS), now), new BigDecimal("0.5032476353"));
        assertEquals(target.calculateConfidence(now.minus(2, DAYS), now), new BigDecimal("0.4844059414"));
        assertEquals(target.calculateConfidence(now.minus(7, DAYS), now), new BigDecimal("0.4503523133"));
        assertEquals(target.calculateConfidence(now.minus(30, DAYS), now), new BigDecimal("0.4107935049"));
        assertEquals(target.calculateConfidence(now.minus(365, DAYS), now), new BigDecimal("0.3428717976"));
        assertEquals(target.calculateConfidence(now.minus(3650, DAYS), now), new BigDecimal("0.2802810454"));
        assertEquals(target.calculateConfidence(now.minus(365000, DAYS), now), new BigDecimal("0.1550995411"));

    }

}
