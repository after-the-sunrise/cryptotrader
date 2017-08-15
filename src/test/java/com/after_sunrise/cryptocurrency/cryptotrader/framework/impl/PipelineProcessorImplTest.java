package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.MarketEstimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.PortfolioAdviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class PipelineProcessorImplTest {

    private PipelineProcessorImpl target;

    private TestModule module;

    private Context context;

    @BeforeMethod
    public void setUp() {

        module = new TestModule();

        context = module.getMock(Context.class);

        target = new PipelineProcessorImpl(module.createInjector());

    }

    @Test
    public void testAccept() throws Exception {

        Request request = Request.builder().build();
        Estimation estimation = Estimation.builder().build();
        Advice advice = Advice.builder().build();
        CreateInstruction create = CreateInstruction.builder().build();
        CancelInstruction cancel = CancelInstruction.builder().build();
        List<Instruction> instructions = Arrays.asList(create, null, cancel);

        when(module.getMock(MarketEstimator.class).estimate(context, request)).thenReturn(estimation);
        when(module.getMock(PortfolioAdviser.class).advise(context, request, estimation)).thenReturn(advice);
        when(module.getMock(OrderInstructor.class).instruct(context, request, advice)).thenReturn(instructions);

        target.accept(request);

        verify(module.getMock(MarketEstimator.class)).estimate(context, request);
        verify(module.getMock(PortfolioAdviser.class)).advise(context, request, estimation);
        verify(module.getMock(OrderInstructor.class)).instruct(context, request, advice);
        verify(module.getMock(OrderManager.class)).create(request, create);
        verify(module.getMock(OrderManager.class)).cancel(request, cancel);

    }

    @Test
    public void testAccept_NullParameters() throws Exception {

        Request request = null;
        Estimation estimation = null;
        Advice advice = null;
        CreateInstruction create = CreateInstruction.builder().build();
        CancelInstruction cancel = CancelInstruction.builder().build();
        List<Instruction> instructions = Arrays.asList(create, null, cancel);

        when(module.getMock(MarketEstimator.class).estimate(context, request)).thenReturn(estimation);
        when(module.getMock(PortfolioAdviser.class).advise(context, request, estimation)).thenReturn(advice);
        when(module.getMock(OrderInstructor.class).instruct(context, request, advice)).thenReturn(instructions);

        target.accept(request);

        verify(module.getMock(MarketEstimator.class)).estimate(context, request);
        verify(module.getMock(PortfolioAdviser.class)).advise(context, request, estimation);
        verify(module.getMock(OrderInstructor.class)).instruct(context, request, advice);
        verify(module.getMock(OrderManager.class)).create(request, create);
        verify(module.getMock(OrderManager.class)).cancel(request, cancel);

    }

    @Test
    public void testAccept_NullInstructions() throws Exception {

        Request request = null;
        Estimation estimation = null;
        Advice advice = null;
        List<Instruction> instructions = null;

        when(module.getMock(MarketEstimator.class).estimate(context, request)).thenReturn(estimation);
        when(module.getMock(PortfolioAdviser.class).advise(context, request, estimation)).thenReturn(advice);
        when(module.getMock(OrderInstructor.class).instruct(context, request, advice)).thenReturn(instructions);

        target.accept(request);

        verify(module.getMock(MarketEstimator.class)).estimate(context, request);
        verify(module.getMock(PortfolioAdviser.class)).advise(context, request, estimation);
        verify(module.getMock(OrderInstructor.class)).instruct(context, request, advice);
        verifyNoMoreInteractions(module.getMock(OrderManager.class));

    }

}
