package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.math.BigDecimal.valueOf;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class PipelineImplTest {

    private PipelineImpl target;

    private TestModule module;

    private Context context;

    @BeforeMethod
    public void setUp() {

        module = new TestModule();

        context = module.getMock(Context.class);

        target = spy(new PipelineImpl(module.createInjector()));

    }

    @Test
    public void testProcess() throws Exception {

        Instant now = module.getNow();
        String site = "testsite";
        String instrument = "testinstrument";
        Request request = Request.builder().build();
        Estimation estimation = Estimation.builder().build();
        Advice advice = Advice.builder().build();
        List<Instruction> instructions = Collections.emptyList();
        Map<Instruction, String> results = Collections.emptyMap();
        Map<Instruction, Boolean> reconcile = Collections.emptyMap();

        doReturn(request).when(target).createRequest(now, site, instrument);
        when(module.getMock(Estimator.class).estimate(context, request)).thenReturn(estimation);
        when(module.getMock(Adviser.class).advise(context, request, estimation)).thenReturn(advice);
        when(module.getMock(Instructor.class).instruct(context, request, advice)).thenReturn(instructions);
        when(module.getMock(Agent.class).manage(context, request, instructions)).thenReturn(results);
        when(module.getMock(Agent.class).reconcile(context, request, results)).thenReturn(reconcile);

        target.process(now, site, instrument);

        verify(module.getMock(Estimator.class)).estimate(context, request);
        verify(module.getMock(Adviser.class)).advise(context, request, estimation);
        verify(module.getMock(Instructor.class)).instruct(context, request, advice);
        verify(module.getMock(Agent.class)).manage(context, request, instructions);
        verify(module.getMock(Agent.class)).reconcile(context, request, results);

    }

    @Test
    public void testProcess_NullParameters() throws Exception {

        Instant now = null;
        String site = null;
        String instrument = null;
        Request request = null;
        Estimation estimation = null;
        Advice advice = null;
        List<Instruction> instructions = null;
        Map<Instruction, String> results = null;
        Map<Instruction, Boolean> reconcile = null;

        doReturn(request).when(target).createRequest(now, site, instrument);
        when(module.getMock(Estimator.class).estimate(context, request)).thenReturn(estimation);
        when(module.getMock(Adviser.class).advise(context, request, estimation)).thenReturn(advice);
        when(module.getMock(Instructor.class).instruct(context, request, advice)).thenReturn(instructions);
        when(module.getMock(Agent.class).manage(context, request, instructions)).thenReturn(results);
        when(module.getMock(Agent.class).reconcile(context, request, results)).thenReturn(reconcile);

        target.process(now, site, instrument);

        verify(module.getMock(Estimator.class)).estimate(context, request);
        verify(module.getMock(Adviser.class)).advise(context, request, estimation);
        verify(module.getMock(Instructor.class)).instruct(context, request, advice);
        verify(module.getMock(Agent.class)).manage(context, request, instructions);
        verify(module.getMock(Agent.class)).reconcile(context, request, results);

    }

    @Test
    public void testCreateRequest() {

        Instant time = Instant.now();
        String site = "test";
        String instrument = "i";
        when(module.getMock(PropertyManager.class).getTradingSpread()).thenReturn(valueOf(2));
        when(module.getMock(PropertyManager.class).getTradingExposure()).thenReturn(valueOf(3));
        when(module.getMock(PropertyManager.class).getTradingSplit()).thenReturn(valueOf(4));

        Request request = target.createRequest(time, site, instrument);
        assertEquals(request.getTargetTime(), time);
        assertEquals(request.getSite(), site);
        assertEquals(request.getInstrument(), instrument);
        assertEquals(request.getTradingSpread(), valueOf(2));
        assertEquals(request.getTradingExposure(), valueOf(3));
        assertEquals(request.getTradingSplit(), valueOf(4));
        assertTrue(Request.isValid(request));
        assertFalse(Request.isInvalid(request));

    }

}
