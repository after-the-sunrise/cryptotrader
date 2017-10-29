package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Map;

import static java.math.BigDecimal.ONE;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class AdviserImplTest {

    private AdviserImpl target;

    private TestModule module;

    private Adviser service;

    private Context context;

    private Estimation estimation;

    @BeforeMethod
    public void setUp() {

        service = mock(Adviser.class);
        module = new TestModule();
        context = null;
        estimation = null;

        Map<String, Adviser> services = singletonMap("test", service);
        when(module.getMock(ServiceFactory.class).loadMap(Adviser.class)).thenReturn(services);

        target = new AdviserImpl(module.createInjector());

    }

    @Test
    public void testGet() throws Exception {

        assertEquals(target.get(), Service.WILDCARD);

    }

    @Test
    public void testCreate() throws Exception {

        Request.RequestBuilder builder = module.createRequestBuilder();
        Request request = builder.site("test").build();

        // Null return
        when(service.advise(context, request, estimation)).thenReturn(null);
        assertNotNull(target.advise(context, request, estimation));
        verify(service, times(1)).advise(context, request, estimation);

        // Some return
        Advice advice = Advice.builder().build();
        when(service.advise(context, request, estimation)).thenReturn(advice);
        assertSame(target.advise(context, request, estimation), advice);
        verify(service, times(2)).advise(context, request, estimation);

        // Site not found
        request = builder.site(null).build();
        assertNotNull(target.advise(context, request, estimation));
        verifyNoMoreInteractions(service);

    }

    @Test
    public void testCalculateBasis() {

        Estimation estimation = Estimation.builder().price(new BigDecimal("123")).build();
        Advice advice = Advice.builder().buyLimitPrice(new BigDecimal("120"))
                .sellLimitPrice(new BigDecimal("130")).build();

        assertEquals(target.calculateBasis(estimation, advice, Advice::getBuyLimitPrice, ONE.negate())
                , new BigDecimal("0.0243902439"));

        assertEquals(target.calculateBasis(estimation, advice, Advice::getSellLimitPrice, ONE)
                , new BigDecimal("0.0569105691"));

        assertNull(target.calculateBasis(null, advice, Advice::getSellLimitPrice, ONE));
        assertNull(target.calculateBasis(estimation, null, Advice::getSellLimitPrice, ONE));
        assertNull(target.calculateBasis(estimation, advice, a -> null, ONE));
        assertNull(target.calculateBasis(Estimation.builder().price(new BigDecimal("0.0")).build(),
                advice, Advice::getSellLimitPrice, ONE));
        assertNull(target.calculateBasis(Estimation.builder().price(null).build(),
                advice, Advice::getSellLimitPrice, ONE));

    }

}
