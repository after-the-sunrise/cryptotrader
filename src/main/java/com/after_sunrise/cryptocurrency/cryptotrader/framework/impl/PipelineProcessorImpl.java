package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.MarketEstimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.PortfolioAdviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class PipelineProcessorImpl implements PipelineProcessor, Instruction.Visitor<Boolean, Request> {

    private final Context context;

    private final MarketEstimator estimator;

    private final PortfolioAdviser adviser;

    private final OrderInstructor instructor;

    private final OrderManager manager;

    @Inject
    public PipelineProcessorImpl(Injector injector) {

        this.estimator = injector.getInstance(MarketEstimator.class);

        this.context = injector.getInstance(Context.class);

        this.adviser = injector.getInstance(PortfolioAdviser.class);

        this.instructor = injector.getInstance(OrderInstructor.class);

        this.manager = injector.getInstance(OrderManager.class);

    }

    @Override
    public void accept(Request request) {

        Estimation estimation = estimator.estimate(context, request);

        Advice advice = adviser.advise(context, request, estimation);

        List<Instruction> instructions = instructor.instruct(context, request, advice);

        Optional.ofNullable(instructions).ifPresent(l -> l.stream() //
                .filter(Objects::nonNull).forEach(i -> i.accept(this, request)));

    }

    @Override
    public Boolean visit(Request request, Instruction.CreateInstruction instruction) {
        return manager.create(request, instruction);
    }

    @Override
    public Boolean visit(Request request, Instruction.CancelInstruction instruction) {
        return manager.cancel(request, instruction);
    }

}
