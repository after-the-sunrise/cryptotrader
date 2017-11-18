package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class MicroEstimatorTest {

    private MicroEstimator target;

    private Context context;

    @BeforeMethod
    public void setUp() throws Exception {

        context = mock(Context.class);

        target = new MicroEstimator();

    }

    @Test
    public void testGet() throws Exception {
        assertEquals(target.get(), target.getClass().getSimpleName());
    }

    @Test
    public void testEstimate() throws Exception {

        Request request = Request.builder().build();
        Key key = Key.from(request);

        Runnable initializer = () -> {
            reset(context);
            when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("500000"));
            when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("400000"));
            when(context.getBestAskSize(key)).thenReturn(new BigDecimal("100"));
            when(context.getBestBidSize(key)).thenReturn(new BigDecimal("400"));
        };

        // No prices
        reset(context);
        Estimation result = target.estimate(context, request);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), ZERO);

        // No Ask Price
        initializer.run();
        when(context.getBestAskPrice(key)).thenReturn(null);
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), ZERO);

        // No Bid Price
        initializer.run();
        when(context.getBestBidPrice(key)).thenReturn(null);
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), ZERO);

        // No Ask Size
        initializer.run();
        when(context.getBestAskSize(key)).thenReturn(null);
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), ZERO);

        // No Bid Size
        initializer.run();
        when(context.getBestBidSize(key)).thenReturn(null);
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), ZERO);

        // Zero Size
        initializer.run();
        when(context.getBestBidSize(key)).thenReturn(new BigDecimal("-100"));
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), ZERO);

        // Valid
        initializer.run();
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), new BigDecimal("420000.0000000000"));
        assertEquals(result.getConfidence(), new BigDecimal("0.7619047619"));

        // Inverse Price
        initializer.run();
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("300000"));
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), new BigDecimal("380000.0000000000"));
        assertEquals(result.getConfidence(), new BigDecimal("0.7368421053"));

        // Equilibrium
        initializer.run();
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("400000"));
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), new BigDecimal("400000.0000000000"));
        assertEquals(result.getConfidence(), new BigDecimal("1.0000000000"));

        // Zero Price
        initializer.run();
        when(context.getBestAskPrice(key)).thenReturn(new BigDecimal("+400000"));
        when(context.getBestBidPrice(key)).thenReturn(new BigDecimal("-100000"));
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), new BigDecimal("0.0000000000"));
        assertEquals(result.getConfidence(), new BigDecimal("0.5"));

    }

}
