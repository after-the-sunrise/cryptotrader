package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import org.apache.commons.configuration2.MapConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
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

    @BeforeMethod
    public void setUp() throws Exception {
        target = new TestEstimator();
    }

    @Test
    public void testGetKey() throws Exception {

        Request r = Request.builder().build();

        assertEquals(target.getKey(r), Context.Key.from(r));

    }

    @Test
    public void testConfiguration() {

        Map<String, Object> map = new HashMap<>();
        target.setConfiguration(new MapConfiguration(map));

        // Int
        assertEquals(target.getIntConfiguration("int", -123), -123);
        map.put(target.getClass().getName() + ".int", -999);
        assertEquals(target.getIntConfiguration("int", -123), -999);
        map.put(target.getClass().getName() + ".int", "a"); // Exception
        assertEquals(target.getIntConfiguration("int", -123), -123);

        // Decimal
        assertEquals(target.getDecimalConfiguration("decimal", TEN), TEN);
        map.put(target.getClass().getName() + ".decimal", "1");
        assertEquals(target.getDecimalConfiguration("decimal", TEN), ONE);
        map.put(target.getClass().getName() + ".decimal", "a"); // Exception
        assertEquals(target.getDecimalConfiguration("decimal", TEN), TEN);

        // String
        assertEquals(target.getStringConfiguration("string", "b"), "b");
        map.put(target.getClass().getName() + ".string", "a");
        assertEquals(target.getStringConfiguration("string", "b"), "a");
        map.put(target.getClass().getName() + ".string", new Object() {
            @Override
            public String toString() {
                throw new RuntimeException("test");
            }
        }); // Exception
        assertEquals(target.getStringConfiguration("string", "b"), "b");

    }

}
