package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;

import static java.math.BigDecimal.TEN;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class ContextProviderTest {

    private ContextProvider target;

    private TestModule module;

    private Context service;

    @BeforeMethod
    public void setUp() {

        service = mock(Context.class);
        module = new TestModule();

        Map<String, Context> services = Collections.singletonMap("test", service);
        when(module.getMock(ServiceFactory.class).loadMap(Context.class)).thenReturn(services);

        target = new ContextProvider(module.createInjector());

    }

    @Test
    public void testGet() throws Exception {

        Context.Key.KeyBuilder builder = Context.Key.builder().site("test");

        Context.Key key = builder.build();

        // Found
        when(service.getMidPrice(key)).thenReturn(TEN);
        assertEquals(target.get().getMidPrice(key), TEN);
        verify(service, times(1)).getMidPrice(key);

        // Null result
        when(service.getMidPrice(key)).thenReturn(null);
        assertNull(target.get().getMidPrice(key));
        verify(service, times(2)).getMidPrice(key);

        // Not found
        key = builder.site("hoge").build();
        assertNull(target.get().getMidPrice(key));
        verifyNoMoreInteractions(service);

        // Method without key
        assertNull(target.get().toString());
        verifyNoMoreInteractions(service);

    }

}
