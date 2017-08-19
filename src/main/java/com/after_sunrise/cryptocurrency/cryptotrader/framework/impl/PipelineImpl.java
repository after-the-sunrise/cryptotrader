package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class PipelineImpl implements Pipeline {

    private final PropertyManager propertyManager;

    private final Context context;

    private final Estimator estimator;

    private final Adviser adviser;

    private final Instructor instructor;

    private final Agent manager;

    @Inject
    public PipelineImpl(Injector injector) {

        this.propertyManager = injector.getInstance(PropertyManager.class);

        this.context = injector.getInstance(Context.class);

        this.estimator = injector.getInstance(Estimator.class);

        this.adviser = injector.getInstance(Adviser.class);

        this.instructor = injector.getInstance(Instructor.class);

        this.manager = injector.getInstance(Agent.class);

    }

    @Override
    public void process(Instant time, String site, String instrument) {

        Request request = createRequest(time, site, instrument);

        log.debug("Processing : {}", request);

        Estimation estimation = estimator.estimate(context, request);

        Advice advice = adviser.advise(context, request, estimation);

        List<Instruction> instructions = instructor.instruct(context, request, advice);

        Map<Instruction, String> futures = manager.manage(context, request, instructions);

        Map<Instruction, Boolean> results = manager.reconcile(context, request, futures);

        log.debug("Processed : {}", ofNullable(results).orElse(emptyMap()).size());

    }

    @VisibleForTesting
    Request createRequest(Instant time, String site, String instrument) {

        Request.RequestBuilder builder = Request.builder();

        builder = builder.timestamp(time).site(site).instrument(instrument);

        builder.tradingSpread(propertyManager.getTradingSpread());

        builder.tradingExposure(propertyManager.getTradingExposure());

        builder.tradingSplit(propertyManager.getTradingSplit());

        return builder.build();

    }

}
