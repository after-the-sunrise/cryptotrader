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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

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

        Context.Key k1 = Context.Key.builder().site("a").instrument("1").timestamp(request.getCurrentTime()).build();
        Context.Key k2 = Context.Key.builder().site("b").instrument("2").timestamp(request.getCurrentTime()).build();
        Context.Key k3 = Context.Key.builder().site("b").instrument("3").timestamp(request.getCurrentTime()).build();
        Context.Key k4 = Context.Key.builder().site("c").instrument("4").timestamp(request.getCurrentTime()).build();

        Estimator.Estimation e1 = Estimator.Estimation.builder().price(new BigDecimal("0.1")).confidence(new BigDecimal("0.9")).build();
        Estimator.Estimation e2 = Estimator.Estimation.builder().price(new BigDecimal("0.2")).confidence(new BigDecimal("0.8")).build();
        Estimator.Estimation e3 = Estimator.Estimation.builder().price(new BigDecimal("0.3")).confidence(new BigDecimal("0.7")).build();
        Estimator.Estimation e4 = Estimator.Estimation.builder().price(new BigDecimal("0.4")).confidence(new BigDecimal("0.6")).build();

        BiFunction<Context, Context.Key, Estimator.Estimation> function = mock(BiFunction.class);
        when(function.apply(context, k1)).thenReturn(e1);
        when(function.apply(context, k2)).thenReturn(e2);
        when(function.apply(context, k3)).thenReturn(e3);
        when(function.apply(context, k4)).thenReturn(e4);

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
        when(function.apply(context, k2)).thenReturn(Estimator.Estimation.builder().price(ONE).build());
        result = target.estimate(context, request, function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Null price
        when(function.apply(context, k2)).thenReturn(Estimator.Estimation.builder().confidence(ONE).build());
        result = target.estimate(context, request, function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Null estimation
        when(function.apply(context, k2)).thenReturn(null);
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
