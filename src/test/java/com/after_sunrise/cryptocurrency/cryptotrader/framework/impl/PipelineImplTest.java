package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.math.BigDecimal.valueOf;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

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

        Request request = module.createRequestBuilder().build();
        String site = request.getSite();
        String instrument = request.getInstrument();
        Instant now = request.getCurrentTime();
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

        doReturn(null).when(target).createRequest(any(), any(), any());

        target.process(null, null, null);

        verifyZeroInteractions(
                module.getMock(Estimator.class),
                module.getMock(Adviser.class),
                module.getMock(Instructor.class),
                module.getMock(Agent.class),
                module.getMock(Agent.class)
        );

    }

    @Test
    public void testCreateRequest() {

        String site = "test";
        String instrument = "i";
        Instant currentTime = Instant.now();
        Instant targetTime = currentTime.plus(Duration.ofMillis(5L));

        Runnable initializer = () -> {
            when(module.getMock(PropertyManager.class).getNow()).thenReturn(currentTime);
            when(module.getMock(PropertyManager.class).getTradingSpread(any(), any())).thenReturn(valueOf(2));
            when(module.getMock(PropertyManager.class).getTradingExposure(any(), any())).thenReturn(valueOf(3));
            when(module.getMock(PropertyManager.class).getTradingSplit(any(), any())).thenReturn(valueOf(4));
            when(module.getMock(PropertyManager.class).getFundingOffset(any(), any())).thenReturn(valueOf(5));
        };

        initializer.run();
        Request request = target.createRequest(targetTime, site, instrument);
        assertEquals(request.getSite(), site);
        assertEquals(request.getInstrument(), instrument);
        assertEquals(request.getCurrentTime(), currentTime);
        assertEquals(request.getTargetTime(), targetTime);
        assertEquals(request.getTradingSpread(), valueOf(2));
        assertEquals(request.getTradingExposure(), valueOf(3));
        assertEquals(request.getTradingSplit(), valueOf(4));
        assertEquals(request.getFundingOffset(), valueOf(5));

        // Null Argument
        assertNull(target.createRequest(null, site, instrument));
        assertNull(target.createRequest(targetTime, null, instrument));
        assertNull(target.createRequest(targetTime, site, null));

        initializer.run();
        doReturn(null).when(module.getMock(PropertyManager.class)).getNow();
        assertNull(target.createRequest(targetTime, site, instrument));

        initializer.run();
        doReturn(null).when(module.getMock(PropertyManager.class)).getTradingSpread(any(), any());
        assertNull(target.createRequest(targetTime, site, instrument));

        initializer.run();
        doReturn(null).when(module.getMock(PropertyManager.class)).getTradingExposure(any(), any());
        assertNull(target.createRequest(targetTime, site, instrument));

        initializer.run();
        doReturn(null).when(module.getMock(PropertyManager.class)).getTradingSplit(any(), any());
        assertNull(target.createRequest(targetTime, site, instrument));

        initializer.run();
        doReturn(null).when(module.getMock(PropertyManager.class)).getFundingOffset(any(), any());
        assertNull(target.createRequest(targetTime, site, instrument));

    }

}
