package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = PRIVATE)
public class Request {

    private String site;

    private String instrument;

    private Instant currentTime;

    private Instant targetTime;

    private BigDecimal tradingSpread;

    private BigDecimal tradingSpreadAsk;

    private BigDecimal tradingSpreadBid;

    private BigDecimal tradingSigma;

    private Integer tradingSamples;

    private BigDecimal tradingExposure;

    private BigDecimal tradingThreshold;

    private BigDecimal tradingMaximum;

    private BigDecimal tradingMinimum;

    private BigDecimal tradingResistance;

    private BigDecimal tradingAversion;

    private String tradingInstruction;

    private Integer tradingSplit;

    private Duration tradingDuration;

    private BigDecimal fundingOffset;

    private Map<String, Set<String>> fundingMultiplierProducts;

    private BigDecimal fundingPositiveMultiplier;

    private BigDecimal fundingNegativeMultiplier;

    private BigDecimal fundingPositiveThreshold;

    private BigDecimal fundingNegativeThreshold;

    private Map<String, Set<String>> hedgeProducts;

    private Map<String, Set<String>> estimatorComposites;

    private BigDecimal estimationAversion;

    public static Request.RequestBuilder build(Request request) {

        Request.RequestBuilder b = builder();

        if (request != null) {
            b.site(request.getSite());
            b.instrument(request.getInstrument());
            b.currentTime(request.getCurrentTime());
            b.targetTime(request.getTargetTime());
            b.tradingSpread(request.getTradingSpread());
            b.tradingSpreadAsk(request.getTradingSpreadAsk());
            b.tradingSpreadBid(request.getTradingSpreadBid());
            b.tradingSigma(request.getTradingSigma());
            b.tradingSamples(request.getTradingSamples());
            b.tradingExposure(request.getTradingExposure());
            b.tradingThreshold(request.getTradingThreshold());
            b.tradingMaximum(request.getTradingMinimum());
            b.tradingMinimum(request.getTradingMinimum());
            b.tradingResistance(request.getTradingResistance());
            b.tradingAversion(request.getTradingAversion());
            b.tradingInstruction(request.getTradingInstruction());
            b.tradingSplit(request.getTradingSplit());
            b.tradingDuration(request.getTradingDuration());
            b.fundingOffset(request.getFundingOffset());
            b.fundingMultiplierProducts(request.getFundingMultiplierProducts());
            b.fundingPositiveMultiplier(request.getFundingPositiveMultiplier());
            b.fundingNegativeMultiplier(request.getFundingNegativeMultiplier());
            b.fundingPositiveThreshold(request.getFundingPositiveThreshold());
            b.fundingNegativeThreshold(request.getFundingNegativeThreshold());
            b.hedgeProducts(request.getHedgeProducts());
            b.estimatorComposites(request.getEstimatorComposites());
            b.estimationAversion(request.getEstimationAversion());
        }

        return b;

    }

}
