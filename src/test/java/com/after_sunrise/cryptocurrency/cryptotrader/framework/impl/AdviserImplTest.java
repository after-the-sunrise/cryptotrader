package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request.ALL;
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

        assertEquals(target.get(), ALL);

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

}
