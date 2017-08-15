package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.OrderInstructor;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.PortfolioAdviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request.ALL;
import static java.math.BigDecimal.ZERO;
import static java.time.Instant.now;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class OrderInstructorImplTest {

    private OrderInstructorImpl target;

    private TestModule module;

    private OrderInstructor service;

    private Context context;

    private Advice advice;

    @BeforeMethod
    public void setUp() {

        service = mock(OrderInstructor.class);
        module = new TestModule();
        context = null;
        advice = null;

        Map<String, OrderInstructor> services = singletonMap("test", service);
        when(module.getMock(ServiceFactory.class).loadMap(OrderInstructor.class)).thenReturn(services);

        target = new OrderInstructorImpl(module.createInjector());

    }

    @Test
    public void testGet() throws Exception {

        assertEquals(target.get(), ALL);

    }

    @Test
    public void testInstruct() throws Exception {

        Request.RequestBuilder builder = Request.builder().site("test").instrument("i") //
                .timestamp(now()).aggressiveness(ZERO);

        Request request = builder.build();

        // Null return
        when(service.instruct(context, request, advice)).thenReturn(null);
        List<Instruction> instructions = target.instruct(context, request, advice);
        assertEquals(instructions.size(), 0);
        verify(service, times(1)).instruct(context, request, advice);

        // Some return
        List<Instruction> result = singletonList(CreateInstruction.builder().build());
        when(service.instruct(context, request, advice)).thenReturn(result);
        instructions = target.instruct(context, request, advice);
        assertEquals(instructions, result);
        verify(service, times(2)).instruct(context, request, advice);

        // Site not found
        request = builder.site("hoge").build();
        instructions = target.instruct(context, request, advice);
        assertEquals(instructions.size(), 0);
        verifyNoMoreInteractions(service);

        // Invalid request
        instructions = target.instruct(context, null, advice);
        assertEquals(instructions.size(), 0);
        verifyNoMoreInteractions(service);

    }

}
