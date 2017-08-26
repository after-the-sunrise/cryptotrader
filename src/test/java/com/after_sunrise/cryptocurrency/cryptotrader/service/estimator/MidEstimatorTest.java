package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class MidEstimatorTest {

    private MidEstimator target;

    private Context context;

    @BeforeMethod
    public void setUp() throws Exception {

        context = mock(Context.class);

        target = new MidEstimator();

    }

    @Test
    public void testGet() throws Exception {
        assertEquals(target.get(), target.getClass().getSimpleName());
    }

    @Test
    public void testEstimate() throws Exception {

        Request request = Request.builder().build();
        Key key = Key.from(request);

        // No prices
        Estimation result = target.estimate(context, request);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), ZERO);

        // Valid
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("470200"));
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("470100"));
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), new BigDecimal("470150.0"));
        assertEquals(result.getConfidence(), new BigDecimal("0.999787301925"));

        // Valid (Wide)
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("470200"));
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("400000"));
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), new BigDecimal("435100.0"));
        assertEquals(result.getConfidence(), new BigDecimal("0.838657779821"));

        // Inverse Price
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("470100"));
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("470200"));
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), new BigDecimal("470150.0"));
        assertEquals(result.getConfidence(), new BigDecimal("0.999787301925"));

        // Equilibrium
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("470100"));
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("470100"));
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), new BigDecimal("470100.0"));
        assertEquals(result.getConfidence(), new BigDecimal("1.000000000000"));

        // Zero Mid
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("+450000"));
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("-450000"));
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), new BigDecimal("0.0"));
        assertEquals(result.getConfidence(), new BigDecimal("0.5"));

        // Extreme Spread
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("+450000.1"));
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("-450000"));
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), new BigDecimal("0.05"));
        assertEquals(result.getConfidence(), new BigDecimal("0.5"));

    }

}
