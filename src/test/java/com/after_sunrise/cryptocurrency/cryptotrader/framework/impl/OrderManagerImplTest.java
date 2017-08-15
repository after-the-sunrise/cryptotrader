package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.OrderManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request.ALL;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ZERO;
import static java.time.Instant.now;
import static java.util.Collections.singletonMap;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class OrderManagerImplTest {

    private OrderManagerImpl target;

    private TestModule module;

    private OrderManager service;

    @BeforeMethod
    public void setUp() {

        service = mock(OrderManager.class);
        module = new TestModule();

        Map<String, OrderManager> services = singletonMap("test", service);
        when(module.getMock(ServiceFactory.class).loadMap(OrderManager.class)).thenReturn(services);

        target = new OrderManagerImpl(module.createInjector());

    }

    @Test
    public void testGet() throws Exception {

        assertEquals(target.get(), ALL);

    }

    @Test
    public void testCreate() throws Exception {

        Request.RequestBuilder builder = Request.builder().site("test").instrument("i") //
                .timestamp(now()).aggressiveness(ZERO);

        Request request = builder.build();

        CreateInstruction instruction = null;

        // Null return
        when(service.create(request, instruction)).thenReturn(null);
        assertFalse(target.create(request, instruction));
        verify(service, times(1)).create(any(Request.class), any(CreateInstruction.class));

        // False
        when(service.create(request, instruction)).thenReturn(FALSE);
        assertFalse(target.create(request, instruction));
        verify(service, times(2)).create(any(Request.class), any(CreateInstruction.class));

        // True
        when(service.create(request, instruction)).thenReturn(TRUE);
        assertTrue(target.create(request, instruction));
        verify(service, times(3)).create(any(Request.class), any(CreateInstruction.class));

        // Site not found
        request = builder.site("hoge").build();
        assertFalse(target.create(request, instruction));
        verifyNoMoreInteractions(service);

        // Invalid request
        assertFalse(target.create(null, instruction));
        verifyNoMoreInteractions(service);

    }

}
