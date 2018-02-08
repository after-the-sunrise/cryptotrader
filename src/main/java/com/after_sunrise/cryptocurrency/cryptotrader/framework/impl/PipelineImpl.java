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

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

            Instant s = propertyManager.getNow();

            Instant i = s;

            Estimation estimation = estimator.estimate(context, request);
            i = logElapsed(i, request, "Estimate");

            Advice advice = adviser.advise(context, request, estimation);
            i = logElapsed(i, request, "Advise");

            List<Instruction> instructions = instructor.instruct(context, request, advice);
            i = logElapsed(i, request, "Instruct");

            Map<Instruction, String> futures = manager.manage(context, request, instructions);
            i = logElapsed(i, request, "Manage");

            Map<Instruction, Boolean> results = manager.reconcile(context, request, futures);
            i = logElapsed(i, request, "Reconcile");

            logElapsed(s, request, "Total");

        });

    }

    @VisibleForTesting
    Instant logElapsed(Instant start, Request request, String label) {

        Instant now = propertyManager.getNow();

        String elapsed = String.valueOf(Duration.between(start, now).getSeconds());

        log.debug("[{}.{}] {} seconds : {}",
                request.getSite(),
                request.getInstrument(),
                StringUtils.leftPad(elapsed, 3, ' '),
                label
        );

        return now;

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
                .tradingThreshold(propertyManager.getTradingThreshold(site, instrument))
                .tradingMaximum(propertyManager.getTradingMaximum(site, instrument))
                .tradingMinimum(propertyManager.getTradingMinimum(site, instrument))
                .tradingResistance(propertyManager.getTradingResistance(site, instrument))
                .tradingAversion(propertyManager.getTradingAversion(site, instrument))
                .tradingInstruction(propertyManager.getTradingInstruction(site, instrument))
                .tradingSplit(propertyManager.getTradingSplit(site, instrument))
                .tradingDuration(propertyManager.getTradingDuration(site, instrument))
                .fundingOffset(propertyManager.getFundingOffset(site, instrument))
                .fundingMultiplierProducts(propertyManager.getFundingMultiplierProducts(site, instrument))
                .fundingPositiveMultiplier(propertyManager.getFundingPositiveMultiplier(site, instrument))
                .fundingNegativeMultiplier(propertyManager.getFundingNegativeMultiplier(site, instrument))
                .fundingPositiveThreshold(propertyManager.getFundingPositiveThreshold(site, instrument))
                .fundingNegativeThreshold(propertyManager.getFundingNegativeThreshold(site, instrument))
                .deviationProducts(propertyManager.getDeviationProducts(site, instrument))
                .aversionProducts(propertyManager.getAversionProducts(site, instrument))
                .hedgeProducts(propertyManager.getHedgeProducts(site, instrument))
                .estimatorComposites(propertyManager.getEstimatorComposites(site, instrument))
                .estimationAversion(propertyManager.getEstimationAversion(site, instrument))
                .build();

        return validateRequest(request);

    }

    @VisibleForTesting
    Request validateRequest(Request request) {

        for (Method m : Request.class.getMethods()) {

            if (!m.getName().startsWith("get")) {
                continue;
            }

            if (m.getParameterCount() != 0) {
                continue;
            }

            try {

                Object value = m.invoke(request);

                Objects.requireNonNull(value);

            } catch (Exception e) {

                log.warn("Invalid Request : " + m.getName());

                return null;

            }

        }

        return request;

    }

}
