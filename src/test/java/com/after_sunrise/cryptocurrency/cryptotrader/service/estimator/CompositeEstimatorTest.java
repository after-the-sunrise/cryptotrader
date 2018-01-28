package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.core.Composite;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.CompositeEstimator.CompositeLastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.CompositeEstimator.CompositeMidEstimator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

        List<Composite> composites = new ArrayList<>();
        composites.add(new Composite("*a", "1"));
        composites.add(new Composite("/b", "2"));
        composites.add(new Composite("/b", "3"));
        composites.add(new Composite("*c", "4"));
        composites.add(new Composite("@d", "5"));
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

            if ("d".equals(r.getSite()) && "5".equals(r.getInstrument())) {
                return b.price(new BigDecimal("0.5")).confidence(new BigDecimal("0.5")).build();
            }

            return null;

        }).when(function).apply(same(context), any());

        CompositeEstimator target = CompositeEstimator.INSTANCE;

        // Multiplied Price = 1 * 0.1 / 0.2 / 0.3 * 0.4 = 0.66666666666...
        // Average Price = (0.666666 + 0.5) / 2
        // Confidence = [(0.9 * 0.8 * 0.7 * 0.6) + 0.5] / 2 = 0.7
        Estimator.Estimation result = target.estimate(context, request, function);
        assertEquals(result.getPrice(), new BigDecimal("0.5833333333"));
        assertEquals(result.getConfidence(), new BigDecimal("0.4012000000"));

        // Composite Only
        composites.clear();
        composites.add(new Composite("*a", "1"));
        composites.add(new Composite("/b", "2"));
        result = target.estimate(context, request, function);
        assertEquals(result.getPrice(), new BigDecimal("0.5000000000"));
        assertEquals(result.getConfidence(), new BigDecimal("0.7200000000"));

        // Average Only
        composites.clear();
        composites.add(new Composite("@c", "4"));
        composites.add(new Composite("@d", "5"));
        result = target.estimate(context, request, function);
        assertEquals(result.getPrice(), new BigDecimal("0.4500000000"));
        assertEquals(result.getConfidence(), new BigDecimal("0.5500000000"));

        // Unknown Operator ('@')
        composites.clear();
        composites.add(new Composite("%a", "1"));
        result = target.estimate(context, request, function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Missing Operator
        composites.clear();
        composites.add(new Composite("a", "1"));
        result = target.estimate(context, request, function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Missing site
        composites.clear();
        composites.add(new Composite(null, "1"));
        result = target.estimate(context, request, function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Missing instrument
        composites.clear();
        composites.add(new Composite("*a", null));
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
