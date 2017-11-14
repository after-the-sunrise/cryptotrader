package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        Optional.ofNullable(createRequest(time, site, instrument)).ifPresent(request -> {

            log.info("Processing : {}", request);

            Estimation estimation = estimator.estimate(context, request);

            Advice advice = adviser.advise(context, request, estimation);

            List<Instruction> instructions = instructor.instruct(context, request, advice);

            Map<Instruction, String> futures = manager.manage(context, request, instructions);

            Map<Instruction, Boolean> results = manager.reconcile(context, request, futures);

            log.info("Processed : {}", ofNullable(results).orElse(emptyMap()).size());

        });

    }

    @VisibleForTesting
    Request createRequest(Instant time, String site, String instrument) {

        Request request = Request.builder()
                .site(site)
                .instrument(instrument)
                .currentTime(propertyManager.getNow())
                .targetTime(time)
                .tradingSpread(propertyManager.getTradingSpread(site, instrument))
                .tradingSpreadAsk(propertyManager.getTradingSpreadAsk(site, instrument))
                .tradingSpreadBid(propertyManager.getTradingSpreadBid(site, instrument))
                .tradingSigma(propertyManager.getTradingSigma(site, instrument))
                .tradingSamples(propertyManager.getTradingSamples(site, instrument))
                .tradingExposure(propertyManager.getTradingExposure(site, instrument))
                .tradingAversion(propertyManager.getTradingAversion(site, instrument))
                .tradingSplit(propertyManager.getTradingSplit(site, instrument))
                .tradingDuration(propertyManager.getTradingDuration(site, instrument))
                .fundingOffset(propertyManager.getFundingOffset(site, instrument))
                .fundingMultiplierProducts(propertyManager.getFundingMultiplierProducts(site, instrument))
                .fundingPositiveMultiplier(propertyManager.getFundingPositiveMultiplier(site, instrument))
                .fundingNegativeMultiplier(propertyManager.getFundingNegativeMultiplier(site, instrument))
                .hedgeProducts(propertyManager.getHedgeProducts(site, instrument))
                .build();

        if (StringUtils.isEmpty(site)) {
            log.warn("Invalid request : site");
            return null;
        }

        if (StringUtils.isEmpty(instrument)) {
            log.warn("Invalid request : instrument");
            return null;
        }

        if (request.getCurrentTime() == null) {
            log.warn("Invalid request : current time");
            return null;
        }

        if (request.getTargetTime() == null) {
            log.warn("Invalid request : target time");
            return null;
        }

        if (request.getTradingSpread() == null) {
            log.warn("Invalid request : trading spread");
            return null;
        }

        if (request.getTradingSpreadAsk() == null) {
            log.warn("Invalid request : trading spread ask");
            return null;
        }

        if (request.getTradingSpreadBid() == null) {
            log.warn("Invalid request : trading spread bid");
            return null;
        }

        if (request.getTradingSigma() == null) {
            log.warn("Invalid request : trading sigma");
            return null;
        }

        if (request.getTradingSamples() == null) {
            log.warn("Invalid request : trading samples");
            return null;
        }

        if (request.getTradingExposure() == null) {
            log.warn("Invalid request : trading exposure");
            return null;
        }

        if (request.getTradingAversion() == null) {
            log.warn("Invalid request : trading aversion");
            return null;
        }

        if (request.getTradingSplit() == null) {
            log.warn("Invalid request : trading split");
            return null;
        }

        if (request.getTradingDuration() == null) {
            log.warn("Invalid request : trading duration");
            return null;
        }

        if (request.getFundingOffset() == null) {
            log.warn("Invalid request : funding offset");
            return null;
        }

        if (request.getFundingMultiplierProducts() == null) {
            log.warn("Invalid request : funding multiplier products");
            return null;
        }

        if (request.getFundingPositiveMultiplier() == null) {
            log.warn("Invalid request : funding positive multiplier");
            return null;
        }

        if (request.getFundingNegativeMultiplier() == null) {
            log.warn("Invalid request : funding negative multiplier");
            return null;
        }

        if (request.getHedgeProducts() == null) {
            log.warn("Invalid request : hedge products");
            return null;
        }

        return request;

    }

}
