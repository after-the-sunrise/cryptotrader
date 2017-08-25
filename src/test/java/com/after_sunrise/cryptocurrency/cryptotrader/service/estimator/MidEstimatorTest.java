package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static java.math.BigDecimal.*;
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

        when(context.getMidPrice(Key.from(request))).thenReturn(TEN);
        Estimation result = target.estimate(context, request);
        assertEquals(result.getPrice(), TEN);
        assertEquals(result.getConfidence(), new BigDecimal("0.5"));

        when(context.getMidPrice(Key.from(request))).thenReturn(null);
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), ZERO);

        when(context.getMidPrice(Key.from(null))).thenReturn(ONE);
        result = target.estimate(context, null);
        assertEquals(result.getPrice(), ONE);
        assertEquals(result.getConfidence(), new BigDecimal("0.5"));

    }

}
