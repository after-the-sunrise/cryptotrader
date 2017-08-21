package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.*;
import static java.util.Optional.ofNullable;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class TemplateAdviser implements Adviser {

    private static final int PRECISION = 12;

    private static final BigDecimal EPSILON = ONE.movePointLeft(PRECISION);

    private static final Advice BAIL = Advice.builder().build();

    private final String id;

    public TemplateAdviser(String id) {
        this.id = id;
    }

    @Override
    public String get() {
        return id;
    }

    @Override
    public Advice advise(Context context, Request request, Estimation estimation) {

        if (!Request.isValid(request)) {

            log.trace("Invalid request : {}", request);

            return BAIL;

        }

        if (!Estimation.isValid(estimation)) {

            log.trace("Invalid estimation : {}", estimation);

            return BAIL;

        }

        BigDecimal bPrice = calculateBuyLimitPrice(context, request, estimation);
        BigDecimal sPrice = calculateSellLimitPrice(context, request, estimation);
        BigDecimal bSize = calculateBuyLimitSize(context, request, bPrice);
        BigDecimal sSize = calculateSellLimitSize(context, request);

        Advice advice = Advice.builder().buyLimitPrice(bPrice).buyLimitSize(bSize) //
                .sellLimitPrice(sPrice).sellLimitSize(sSize).build();

        log.trace("Advice : {} - {}", advice, request);

        return advice;

    }

    BigDecimal calculateWeighedPrice(Context context, Request request, Estimation estimation) {

        Key key = Key.from(request);

        BigDecimal mid = context.getMidPrice(key);

        if (mid == null) {

            log.trace("Weighed price not available. No mid.");

            return null;

        }

        BigDecimal estimate = estimation.getPrice();

        BigDecimal confidence = ONE.min(ZERO.max(estimation.getConfidence()));

        BigDecimal weighed = mid.multiply(ONE.subtract(confidence)).add(estimate.multiply(confidence));

        log.trace("Weighed price : {} (mid=[[]] [{}])", weighed, mid, estimation);

        return weighed;

    }

    @VisibleForTesting
    BigDecimal calculatePositionRatio(Context context, Request request) {

        Key key = Key.from(request);

        BigDecimal funding = ofNullable(context.getFundingPosition(key)).orElse(ZERO);

        BigDecimal structure = ofNullable(context.getInstrumentPosition(key)).orElse(ZERO);

        BigDecimal price = ofNullable(context.getMidPrice(key)).orElse(ZERO);

        BigDecimal equivalent = structure.multiply(price);

        BigDecimal sum = equivalent.add(funding);

        if (sum.signum() == 0) {
            return ZERO;
        }

        BigDecimal diff = equivalent.subtract(funding);

        BigDecimal ratio = diff.divide(sum, PRECISION, HALF_UP);

        log.trace("Position ratio: {} (fund=[{}], structure=[{}] equivalent=[{}])", ratio, structure, equivalent);

        return ratio;

    }

    @VisibleForTesting
    BigDecimal calculateBuyLimitPrice(Context context, Request request, Estimation estimation) {

        Key key = Key.from(request);

        BigDecimal weighed = calculateWeighedPrice(context, request, estimation);

        BigDecimal ask = context.getBestAskPrice(key);

        BigDecimal comm = context.getCommissionRate(key);

        if (weighed == null || ask == null || comm == null) {

            log.trace("Buy price not available : weighed=[{}] ask=[{}] comm=[{}]", weighed, ask, comm);

            return null;

        }

        // TODO : Cost Basis

        BigDecimal adjCross = weighed.min(ask.subtract(EPSILON));

        BigDecimal ratio = calculatePositionRatio(context, request).max(ZERO);

        BigDecimal basis = request.getTradingSpread().multiply(ONE.add(ratio)).add(comm).max(ZERO);

        BigDecimal adjSpread = adjCross.multiply(ONE.subtract(basis));

        BigDecimal rounded = context.roundTickSize(key, adjSpread, DOWN);

        log.trace("Buy price : {} (spread=[{}] basis=[{}])", rounded, adjSpread, basis);

        return rounded;

    }

    @VisibleForTesting
    BigDecimal calculateSellLimitPrice(Context context, Request request, Estimation estimation) {

        Key key = Key.from(request);

        BigDecimal weighed = calculateWeighedPrice(context, request, estimation);

        BigDecimal bid = context.getBestBidPrice(key);

        BigDecimal comm = context.getCommissionRate(key);

        if (weighed == null || bid == null || comm == null) {

            log.trace("Sell price not available : weighed=[{}] bid=[{}] comm=[{}]", weighed, bid, comm);

            return null;

        }

        // TODO : Cost Basis

        BigDecimal adjCross = weighed.max(bid.add(EPSILON));

        BigDecimal ratio = calculatePositionRatio(context, request).min(ZERO).abs();

        BigDecimal basis = request.getTradingSpread().multiply(ONE.add(ratio)).add(comm).max(ZERO);

        BigDecimal adjSpread = adjCross.multiply(ONE.add(basis));

        BigDecimal rounded = context.roundTickSize(key, adjSpread, UP);

        log.trace("Sell price : {} (spread=[{}] basis=[{}])", rounded, adjSpread, basis);

        return rounded;

    }


    @VisibleForTesting
    BigDecimal calculateBuyLimitSize(Context context, Request request, BigDecimal price) {

        if (price == null || price.signum() == 0) {

            log.trace("Invalid buy price : {}", price);

            return ZERO;

        }

        Key key = Key.from(request);

        BigDecimal fund = context.getFundingPosition(key);

        if (fund == null || fund.signum() == 0) {

            log.trace("Fund amount not available : {}", fund);

            return ZERO;

        }

        BigDecimal product = fund.divide(price, PRECISION, DOWN);

        BigDecimal exposure = request.getTradingExposure();

        BigDecimal result = context.roundLotSize(key, product.multiply(exposure), DOWN);

        String message = "Buy size : {} (price=[{}] fund=[{}] product=[{}] exposure=[{}])";

        log.trace(message, result, price, fund, product, exposure);

        return ofNullable(result).orElse(ZERO);

    }

    @VisibleForTesting
    BigDecimal calculateSellLimitSize(Context context, Request request) {

        Key key = Key.from(request);

        BigDecimal position = context.getInstrumentPosition(key);

        if (position == null || position.signum() == 0) {

            log.trace("Position not available : {}");

            return ZERO;

        }

        BigDecimal exposure = request.getTradingExposure();

        BigDecimal exposurePosition = position.multiply(exposure);

        BigDecimal result = context.roundLotSize(key, exposurePosition, DOWN);

        log.trace("Sell size : {} (position=[{}] exposure=[{}])", result, position, exposure);

        return ofNullable(result).orElse(ZERO);

    }

}
