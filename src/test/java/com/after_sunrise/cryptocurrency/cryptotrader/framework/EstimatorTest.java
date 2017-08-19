package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import org.testng.annotations.Test;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class EstimatorTest {

    @Test
    public void testEstimation() throws Exception {

        assertFalse(Estimation.isValid(null));

        Estimation.EstimationBuilder builder = Estimation.builder();
        assertFalse(Estimation.isValid(builder.build()));

        builder = builder.price(ZERO);
        assertFalse(Estimation.isValid(builder.build()));

        builder = builder.confidence(ZERO);
        assertFalse(Estimation.isValid(builder.build()));

        builder = builder.confidence(ONE.negate());
        assertFalse(Estimation.isValid(builder.build()));

        builder = builder.confidence(ONE);
        assertTrue(Estimation.isValid(builder.build()));

    }

}
