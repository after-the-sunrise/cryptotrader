package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class RandomEstimatorTest {

    private RandomEstimator target;

    private Context context;

    @BeforeMethod
    public void setUp() throws Exception {

        context = mock(Context.class);

        target = new RandomEstimator();

    }

    @Test
    public void testGet() throws Exception {
        assertEquals(target.get(), target.getClass().getSimpleName());
    }

    @Test
    public void testEstimate() throws Exception {

        Request request = Request.builder().site("s").instrument("i").build();
        Context.Key key = Context.Key.from(request);
        when(context.getLastPrice(key)).thenReturn(new BigDecimal("1000"));

        for (int i = 0; i < 10; i++) {

            Estimation estimation = target.estimate(context, request);

            BigDecimal price = estimation.getPrice();
            assertTrue(price.compareTo(new BigDecimal("1010")) <= 0, price.toPlainString());
            assertTrue(price.compareTo(new BigDecimal("990")) >= 0, price.toPlainString());

            BigDecimal confidence = estimation.getConfidence();
            assertTrue(confidence.compareTo(ONE) <= 0, price.toPlainString());
            assertTrue(confidence.compareTo(ZERO) >= 0, price.toPlainString());

        }

        when(context.getLastPrice(key)).thenReturn(null);

        Estimation estimation = target.estimate(context, request);
        assertNull(estimation.getPrice());
        assertEquals(estimation.getConfidence(), ZERO);

    }

}
