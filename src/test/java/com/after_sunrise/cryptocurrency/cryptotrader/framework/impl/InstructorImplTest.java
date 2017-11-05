package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instructor;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class InstructorImplTest {

    private InstructorImpl target;

    private TestModule module;

    private Instructor service;

    private Context context;

    private Advice advice;

    @BeforeMethod
    public void setUp() throws Exception {

        service = mock(Instructor.class);
        module = new TestModule();
        context = null;
        advice = null;

        Map<String, Instructor> services = singletonMap("test", service);
        when(module.getMock(ServiceFactory.class).loadMap(Instructor.class)).thenReturn(services);

        target = new InstructorImpl(module.createInjector());

    }

    @Test
    public void testGet() throws Exception {

        assertEquals(target.get(), "*");

    }

    @Test
    public void testInstruct() throws Exception {

        Request.RequestBuilder builder = module.createRequestBuilder();

        Request request = builder.site("test").build();

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
        request = builder.site(null).build();
        instructions = target.instruct(context, request, advice);
        assertEquals(instructions.size(), 0);
        verifyNoMoreInteractions(service);

    }

}
