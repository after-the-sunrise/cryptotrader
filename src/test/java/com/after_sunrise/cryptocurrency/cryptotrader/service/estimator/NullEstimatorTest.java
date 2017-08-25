package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static java.math.BigDecimal.ZERO;
import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class NullEstimatorTest {

    private NullEstimator target;

    @BeforeMethod
    public void setUp() throws Exception {

        target = new NullEstimator();

    }

    @Test
    public void testGet() throws Exception {
        assertEquals(target.get(), target.getClass().getSimpleName());
    }

    @Test
    public void testEstimate() throws Exception {

        Estimation result = target.estimate(null, null);

        assertEquals(result.getPrice(), null);

        assertEquals(result.getConfidence(), ZERO);

    }

}
