package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.CompositeEstimator.CompositeLastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.CompositeEstimator.CompositeMidEstimator;
import com.google.common.collect.Sets;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class CompositeEstimatorTest {

    private Context context;

    @BeforeMethod
    public void setUp() {

        context = mock(Context.class);

    }

    @Test
    public void testCompositeEstimator_Estimate() {

        Request request = Request.builder().build();

        Estimator.Estimation result = CompositeEstimator.INSTANCE.estimate(context, request);

        assertEquals(result.getPrice(), null);

        assertEquals(result.getConfidence(), ZERO);

    }

    @Test
    public void testCompositeEstimator_EstimateComposite() {

        Map<String, Set<String>> composites = new LinkedHashMap<>();
        composites.put("*a", Sets.newHashSet("1"));
        composites.put("/b", Sets.newHashSet("2", "3"));
        composites.put("*c", Sets.newHashSet("4"));
        Request request = Request.builder().currentTime(Instant.now()).estimatorComposites(composites).build();

        BiFunction<Context, Request, Estimator.Estimation> function = mock(BiFunction.class);

        doAnswer(i -> {

            Request r = i.getArgumentAt(1, Request.class);

            if (!request.getCurrentTime().equals(r.getCurrentTime())) {
                fail("Time does not match.");
            }

            Estimator.Estimation.EstimationBuilder b = Estimator.Estimation.builder();

            if ("a".equals(r.getSite()) && "1".equals(r.getInstrument())) {
                return b.price(new BigDecimal("0.1")).confidence(new BigDecimal("0.9")).build();
            }

            if ("b".equals(r.getSite()) && "2".equals(r.getInstrument())) {
                return b.price(new BigDecimal("0.2")).confidence(new BigDecimal("0.8")).build();
            }

            if ("b".equals(r.getSite()) && "3".equals(r.getInstrument())) {
                return b.price(new BigDecimal("0.3")).confidence(new BigDecimal("0.7")).build();
            }

            if ("c".equals(r.getSite()) && "4".equals(r.getInstrument())) {
                return b.price(new BigDecimal("0.4")).confidence(new BigDecimal("0.6")).build();
            }

            return null;

        }).when(function).apply(same(context), any());

        CompositeEstimator target = CompositeEstimator.INSTANCE;

        // Price = 1 * 0.1 / 0.2 / 0.3 * 0.4 = 0.66666666666...
        // Confidence = 0.9 * 0.8 * 0.7 * 0.6 = 0.3024
        Estimator.Estimation result = target.estimate(context, request, function);
        assertEquals(result.getPrice(), new BigDecimal("0.66666666668"));
        assertEquals(result.getConfidence(), new BigDecimal("0.3024"));

        // Unknown Operator ('@')
        composites.clear();
        composites.put("@a", Collections.singleton("1"));
        result = target.estimate(context, request, function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Missing Operator
        composites.clear();
        composites.put("a", Collections.singleton("1"));
        result = target.estimate(context, request, function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Missing site
        composites.clear();
        composites.put(null, Collections.singleton("1"));
        result = target.estimate(context, request, function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Missing instrument
        composites.clear();
        composites.put("*a", Collections.singleton(null));
        result = target.estimate(context, request, function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Null confidence
        when(function.apply(same(context), any())).thenReturn(Estimator.Estimation.builder().price(ONE).build());
        result = target.estimate(context, request, function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Null price
        when(function.apply(same(context), any())).thenReturn(Estimator.Estimation.builder().confidence(ONE).build());
        result = target.estimate(context, request, function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Null estimation
        when(function.apply(same(context), any())).thenReturn(null);
        result = target.estimate(context, request, function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

    }

    @Test
    public void testCompositeMidEstimator() {

        CompositeMidEstimator target = new CompositeMidEstimator();

        assertEquals(target.get(), "CompositeMidEstimator");

        Estimator.Estimation result = target.estimate(context, Context.Key.builder().build());

        assertEquals(result.getPrice(), null);

        assertEquals(result.getConfidence(), ZERO);

    }

    @Test
    public void testCompositeLastEstimator() {

        CompositeLastEstimator target = new CompositeLastEstimator();

        assertEquals(target.get(), "CompositeLastEstimator");

        Estimator.Estimation result = target.estimate(context, Context.Key.builder().build());

        assertEquals(result.getPrice(), null);

        assertEquals(result.getConfidence(), ZERO);

    }

}
