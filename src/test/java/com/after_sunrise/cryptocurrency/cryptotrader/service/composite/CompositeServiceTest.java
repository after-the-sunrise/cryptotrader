package com.after_sunrise.cryptocurrency.cryptotrader.service.composite;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.service.composite.CompositeService.CompositeMidEstimator;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static java.math.BigDecimal.ONE;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class CompositeServiceTest {

    @Test
    public void testCompositeMidEstimator() {

        Instant now = Instant.now();
        Request request = Request.builder().site("ss").instrument("si").currentTime(now).build();
        Context context = mock(Context.class);

        BigDecimal p1 = new BigDecimal("0.1");
        BigDecimal p2 = new BigDecimal("0.2");
        BigDecimal p3 = new BigDecimal("0.3");

        BigDecimal c1 = new BigDecimal("0.9");
        BigDecimal c2 = new BigDecimal("0.8");
        BigDecimal c3 = new BigDecimal("0.7");

        Estimation e1 = Estimation.builder().price(p1).confidence(c1).build();
        Estimation e2 = Estimation.builder().price(p2).confidence(c2).build();
        Estimation e3 = Estimation.builder().price(p3).confidence(c3).build();

        Key k0 = Key.builder().site("*").instrument("a:x|b:y|c:z|d:|e|").timestamp(now).build();
        Key k1 = Key.builder().site("a").instrument("x").timestamp(now).build();
        Key k2 = Key.builder().site("b").instrument("y").timestamp(now).build();
        Key k3 = Key.builder().site("c").instrument("z").timestamp(now).build();

        CompositeMidEstimator target = spy(new CompositeMidEstimator());
        doReturn(k0).when(target).getKey(request);
        doReturn(e1).when(target).estimate(context, k1);
        doReturn(e2).when(target).estimate(context, k2);
        doReturn(e3).when(target).estimate(context, k3);

        Estimation result = target.estimate(context, request);
        assertEquals(result.getPrice(), new BigDecimal("0.006"));
        assertEquals(result.getConfidence(), new BigDecimal("0.504"));

        // Null confidence
        doReturn(Estimation.builder().price(ONE).build()).when(target).estimate(context, k2);
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Null price
        doReturn(Estimation.builder().confidence(ONE).build()).when(target).estimate(context, k2);
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

        // Null estimation
        doReturn(null).when(target).estimate(context, k2);
        result = target.estimate(context, request);
        assertEquals(result.getPrice(), null);
        assertEquals(result.getConfidence(), new BigDecimal("0"));

    }

}
