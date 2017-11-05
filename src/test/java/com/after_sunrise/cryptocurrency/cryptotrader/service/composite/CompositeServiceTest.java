package com.after_sunrise.cryptocurrency.cryptotrader.service.composite;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.service.composite.CompositeService.CompositeEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.composite.CompositeService.CompositeLastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.composite.CompositeService.CompositeMidEstimator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;
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
public class CompositeServiceTest {

    private Context context;

    @BeforeMethod
    public void setUp() {

        context = mock(Context.class);

    }

    @Test
    public void testCompositeEstimator_Estimate() {

        Request request = Request.builder().build();

        CompositeEstimator target = new CompositeEstimator();

        Estimation result = target.estimate(context, request);

        assertEquals(result.getPrice(), null);

        assertEquals(result.getConfidence(), ZERO);

    }

    @Test
    public void testCompositeEstimator_EstimateComposite() {

        Estimation e1 = Estimation.builder().price(new BigDecimal("0.1")).confidence(new BigDecimal("0.9")).build();
        Estimation e2 = Estimation.builder().price(new BigDecimal("0.2")).confidence(new BigDecimal("0.8")).build();
        Estimation e3 = Estimation.builder().price(new BigDecimal("0.3")).confidence(new BigDecimal("0.7")).build();
        Estimation e4 = Estimation.builder().price(new BigDecimal("0.4")).confidence(new BigDecimal("0.6")).build();

        String i = "+a:1|-b:2|*c:3|/d:4";
        Key k0 = Key.builder().site("*").instrument(i).timestamp(Instant.now()).build();
        Key k1 = Key.builder().site("a").instrument("1").timestamp(k0.getTimestamp()).build();
        Key k2 = Key.builder().site("b").instrument("2").timestamp(k0.getTimestamp()).build();
        Key k3 = Key.builder().site("c").instrument("3").timestamp(k0.getTimestamp()).build();
        Key k4 = Key.builder().site("d").instrument("4").timestamp(k0.getTimestamp()).build();

        BiFunction<Context, Key, Estimation> function = mock(BiFunction.class);
        when(function.apply(context, k1)).thenReturn(e1);
        when(function.apply(context, k2)).thenReturn(e2);
        when(function.apply(context, k3)).thenReturn(e3);
        when(function.apply(context, k4)).thenReturn(e4);

        CompositeEstimator target = new CompositeEstimator();

        // Price = (((1 + 0.1) - 0.2) * 0.3) / 0.4 = 0.675
        Estimation result = target.estimateComposite(context, k0, function);
        assertEquals(result.getPrice(), new BigDecimal("0.6750000000"));
        assertEquals(result.getConfidence(), new BigDecimal("0.3024"));

        // Unknown Operator ('@')
        result = target.estimateComposite(context, Key.build(k0).instrument(i.replace('-', '@')).build(), function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Missing Operator
        result = target.estimateComposite(context, Key.build(k0).instrument(i.replace("-", "")).build(), function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Missing site
        result = target.estimateComposite(context, Key.build(k0).instrument(i.replace("b", "")).build(), function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Invalid site:instrument
        result = target.estimateComposite(context, Key.build(k0).instrument(i.replace("-b", "")).build(), function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Null confidence
        when(function.apply(context, k2)).thenReturn(Estimation.builder().price(ONE).build());
        result = target.estimateComposite(context, k0, function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Null price
        when(function.apply(context, k2)).thenReturn(Estimation.builder().confidence(ONE).build());
        result = target.estimateComposite(context, k0, function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Null estimation
        when(function.apply(context, k2)).thenReturn(null);
        result = target.estimateComposite(context, k0, function);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

    }

    @Test
    public void testCompositeMidEstimator() {

        CompositeMidEstimator target = new CompositeMidEstimator();

        Estimation result = target.estimate(context, Key.builder().build());

        assertEquals(result.getPrice(), null);

        assertEquals(result.getConfidence(), ZERO);

    }

    @Test
    public void testCompositeLastEstimator() {

        CompositeLastEstimator target = new CompositeLastEstimator();

        Estimation result = target.estimate(context, Key.builder().build());

        assertEquals(result.getPrice(), null);

        assertEquals(result.getConfidence(), ZERO);

    }

}
