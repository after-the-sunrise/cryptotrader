package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key.from;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class DepthEstimatorTest {

    private DepthEstimator target;

    private Context context;

    @BeforeMethod
    public void setUp() {

        context = mock(Context.class);

        target = spy(new DepthEstimator());

    }

    @Test
    public void testEstimate() throws Exception {

        Request request = Request.builder().site("a").instrument("b").build();

        Runnable initializer = () -> {

            doReturn(new BigDecimal("0.1")).when(target).calculateDeviation(context, request);
            doReturn(new BigDecimal("200")).when(context).getMidPrice(from(request));

            Map<BigDecimal, BigDecimal> asks = new HashMap<>();
            asks.put(null, new BigDecimal("01")); // Exclude
            asks.put(new BigDecimal("222"), new BigDecimal("12")); // Exclude
            asks.put(new BigDecimal("221"), new BigDecimal("43")); // Exclude
            asks.put(new BigDecimal("220"), new BigDecimal("56"));
            asks.put(new BigDecimal("219"), new BigDecimal("87"));
            asks.put(new BigDecimal("218"), new BigDecimal("90"));
            asks.put(new BigDecimal("217"), null); // Exclude
            doReturn(asks).when(context).getAskPrices(from(request));

            Map<BigDecimal, BigDecimal> bids = new HashMap<>();
            bids.put(new BigDecimal("183"), null); // Exclude
            bids.put(new BigDecimal("182"), new BigDecimal("12"));
            bids.put(new BigDecimal("181"), new BigDecimal("43"));
            bids.put(new BigDecimal("180"), new BigDecimal("56"));
            bids.put(new BigDecimal("179"), new BigDecimal("87")); // Exclude
            bids.put(new BigDecimal("178"), new BigDecimal("90")); // Exclude
            bids.put(null, new BigDecimal("01")); // Exclude
            doReturn(bids).when(context).getBidPrices(from(request));

        };

        // VWAP = 206.51162790697674
        // Predict = 200 + (200 - VWAP) = 193.48837209302326
        initializer.run();
        Estimation result = target.estimate(context, request);
        assertEquals(result.getPrice(), new BigDecimal("193.48837209302326"));
        assertEquals(result.getConfidence(), new BigDecimal("0.45"));

    }

}
