package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class AbstractEstimatorTest {

    static class TestEstimator extends AbstractEstimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return BAIL;
        }
    }

    private AbstractEstimator target;

    private Context context;

    @BeforeMethod
    public void setUp() throws Exception {

        context = mock(Context.class);

        target = new TestEstimator();

    }

    @Test
    public void testGetKey() throws Exception {

        Request r = Request.builder().build();

        assertEquals(target.getKey(null, r), Context.Key.from(r));

    }

}
