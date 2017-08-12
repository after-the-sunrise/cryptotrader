package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.MarketEstimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.PortfolioAdviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.OrderInstructor.Context;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class PipelineProcessorImpl implements PipelineProcessor, Instruction.Visitor<Void, Request> {

    private final MarketEstimator estimator;

    private final MarketEstimator.Context marketContext;

    private final PortfolioAdviser adviser;

    private final PortfolioAdviser.Context portfolioContext;

    private final OrderInstructor instructor;

    private final OrderInstructor.Context instructionContext;

    private final OrderManager manager;

    @Inject
    public PipelineProcessorImpl(Injector injector) {

        this.estimator = injector.getInstance(MarketEstimator.class);

        this.marketContext = injector.getInstance(MarketEstimator.Context.class);

        this.adviser = injector.getInstance(PortfolioAdviser.class);

        this.portfolioContext = injector.getInstance(PortfolioAdviser.Context.class);

        this.instructor = injector.getInstance(OrderInstructor.class);

        this.instructionContext = injector.getInstance(Context.class);

        this.manager = injector.getInstance(OrderManager.class);

    }

    @Override
    public void accept(Request request) {

        Estimation estimation = estimator.estimate(marketContext, request);

        Advice advice = adviser.advise(portfolioContext, request, estimation);

        List<Instruction> instructions = instructor.instruct(instructionContext, request, advice);

        instructions.forEach(i -> i.accept(this, request));

    }

    @Override
    public Void visit(Request parameter, Instruction.CreateInstruction instruction) {
        return manager.create(instruction);
    }

    @Override
    public Void visit(Request parameter, Instruction.CancelInstruction instruction) {
        return manager.cancel(instruction);
    }

}
