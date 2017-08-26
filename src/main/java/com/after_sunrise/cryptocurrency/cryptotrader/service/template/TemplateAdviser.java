package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Function;

import static java.lang.Boolean.TRUE;
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

    private static final Function<BigDecimal, BigDecimal> TRIM_ZERO = b -> ofNullable(b).orElse(null);

    private static final BigDecimal EPSILON = ONE.movePointLeft(SCALE);

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

        BigDecimal weighedPrice = calculateWeighedPrice(context, request, estimation);

        BigDecimal basis = calculateBasis(context, request);

        BigDecimal bPrice = calculateBuyLimitPrice(context, request, weighedPrice, basis);

        BigDecimal sPrice = calculateSellLimitPrice(context, request, weighedPrice, basis);

        BigDecimal bSize = calculateBuyLimitSize(context, request, bPrice);

        BigDecimal sSize = calculateSellLimitSize(context, request, sPrice);

        Advice advice = Advice.builder().buyLimitPrice(bPrice).buyLimitSize(bSize) //
                .sellLimitPrice(sPrice).sellLimitSize(sSize).build();

        log.trace("Advice : {} - {}", advice, request);

        return advice;

    }

    @VisibleForTesting
    BigDecimal calculateWeighedPrice(Context context, Request request, Estimation estimation) {

        BigDecimal confidence = estimation.getConfidence();

        if (estimation.getPrice() == null || confidence == null
                || confidence.signum() <= 0 || confidence.compareTo(ONE) > 0) {

            log.trace("Invalid estimation : {}", estimation);

            return null;

        }

        Key key = Key.from(request);

        BigDecimal mid = context.getMidPrice(key);

        if (mid == null) {

            log.trace("Weighed price not available. No mid.");

            return null;

        }

        BigDecimal estimate = estimation.getPrice();

        BigDecimal weighed = mid.multiply(ONE.subtract(confidence)).add(estimate.multiply(confidence));

        log.trace("Weighed price : {} (mid=[[]] [{}])", weighed, mid, estimation);

        return weighed;

    }

    @VisibleForTesting
    BigDecimal calculateBasis(Context context, Request request) {

        Key key = Key.from(request);

        BigDecimal comm = context.getCommissionRate(key);

        if (comm == null) {

            log.trace("Basis not available. Null commission.");

            return null;
        }

        BigDecimal spread = request.getTradingSpread();

        if (spread == null) {

            log.trace("Basis not available. Null spread.");

            return null;
        }

        return spread.add(comm);

    }

    @VisibleForTesting
    BigDecimal calculatePositionRatio(Context context, Request request) {

        Key key = Key.from(request);

        BigDecimal mid = context.getMidPrice(key);

        BigDecimal funding = context.getFundingPosition(key);

        BigDecimal structure = context.getInstrumentPosition(key);

        if (mid == null || funding == null || structure == null) {

            log.trace("Position ratio unavailable : price=[{}] funding=[{}] structure=[{}]", mid, funding, structure);

            return null;

        }

        BigDecimal equivalent = structure.multiply(mid);

        BigDecimal ratio;

        if (Objects.equals(TRUE, context.isMarginable(key))) {

            if (funding.signum() == 0) {
                return ZERO;
            }

            // Leveraged short can be larger than the funding.
            ratio = equivalent.divide(funding, SCALE, HALF_UP);

        } else {

            BigDecimal sum = equivalent.add(funding);

            if (sum.signum() == 0) {
                return ZERO;
            }

            BigDecimal diff = equivalent.subtract(funding);

            ratio = diff.add(diff).divide(sum, SCALE, HALF_UP);

        }

        log.trace("Position ratio: {} (fund=[{}], structure=[{}] equivalent=[{}])", ratio, structure, equivalent);

        return ratio;

    }

    @VisibleForTesting
    BigDecimal calculateBuyLimitPrice(Context context, Request request, BigDecimal weighedPrice, BigDecimal basis) {

        if (weighedPrice == null || basis == null) {

            log.trace("Buy price not available : weighed=[{}] basis=[{}]", weighedPrice, basis);

            return null;

        }

        Key key = Key.from(request);

        BigDecimal ask = context.getBestAskPrice(key);

        if (ask == null) {

            log.trace("Buy price not available : No ask price.");

            return null;

        }

        BigDecimal ratio = calculatePositionRatio(context, request);

        if (ratio == null) {

            log.trace("Buy price not available : No position ratio.");

            return null;

        }

        BigDecimal ratioBasis = basis.multiply(ONE.add(ratio.max(ZERO)));

        BigDecimal ratioPrice = weighedPrice.multiply(ONE.subtract(ratioBasis));

        BigDecimal boundPrice = ratioPrice.min(ask.subtract(EPSILON));

        BigDecimal rounded = context.roundTickSize(key, boundPrice, DOWN);

        log.trace("Buy price : {} (target=[{}] basis=[{}])", rounded, boundPrice, ratioBasis);

        return rounded;

    }

    @VisibleForTesting
    BigDecimal calculateSellLimitPrice(Context context, Request request, BigDecimal weighedPrice, BigDecimal basis) {

        if (weighedPrice == null || basis == null) {

            log.trace("Sell price not available : weighed=[{}] basis=[{}]", weighedPrice, basis);

            return null;

        }

        Key key = Key.from(request);

        BigDecimal bid = context.getBestBidPrice(key);

        if (bid == null) {

            log.trace("Buy price not available : No bid price.");

            return null;

        }

        BigDecimal ratio = calculatePositionRatio(context, request);

        if (ratio == null) {

            log.trace("Sell price not available : No position ratio.");

            return null;

        }

        BigDecimal ratioBasis = basis.multiply(ONE.add(ratio.min(ZERO).abs()));

        BigDecimal ratioPrice = weighedPrice.multiply(ONE.add(ratioBasis));

        BigDecimal boundPrice = ratioPrice.max(bid.add(EPSILON));

        BigDecimal rounded = context.roundTickSize(key, boundPrice, UP);

        log.trace("Sell price : {} (target=[{}] basis=[{}])", rounded, boundPrice, ratioBasis);

        return rounded;

    }

    @VisibleForTesting
    BigDecimal calculateFundingLimitSize(Context context, Request request, BigDecimal price) {

        if (price == null || price.signum() == 0) {

            log.trace("No funding limit size. Price : {}", price);

            return ZERO;

        }

        Key key = Key.from(request);

        BigDecimal fund = context.getFundingPosition(key);

        if (fund == null) {

            log.trace("No funding limit size. Null funding position.");

            return ZERO;

        }

        BigDecimal product = fund.divide(price, SCALE, DOWN);

        BigDecimal exposure = ofNullable(request.getTradingExposure()).orElse(ZERO);

        BigDecimal exposed = product.multiply(exposure);

        BigDecimal rounded = context.roundLotSize(key, exposed, DOWN);

        log.trace("Funding limit size : {} (exposure=[{}] fund=[{}] price=[{}])", rounded, exposure, fund, price);

        return ofNullable(rounded).orElse(ZERO);

    }

    @VisibleForTesting
    BigDecimal calculateBuyLimitSize(Context context, Request request, BigDecimal price) {

        BigDecimal limitSize = calculateFundingLimitSize(context, request, price);

        Key key = Key.from(request);

        if (!Objects.equals(TRUE, context.isMarginable(key))) {

            log.trace("Cash Buy size : {}", limitSize);

            return limitSize;

        }

        if (limitSize == null) {

            log.trace("Margin buy size not not available. Null limit size.");

            return ZERO;

        }

        BigDecimal position = context.getInstrumentPosition(key);

        if (position == null) {

            log.trace("Margin buy size not not available. Null position.");

            return ZERO;

        }

        BigDecimal shortPosition = position.min(ZERO);

        BigDecimal netSize = limitSize.subtract(shortPosition);

        log.trace("Margin Buy size : {} (position=[{}] funding=[{}])", netSize, position, limitSize);

        return netSize;

    }

    protected BigDecimal calculateSellLimitSize(Context context, Request request, BigDecimal price) {

        Key key = Key.from(request);

        BigDecimal position = context.getInstrumentPosition(key);

        if (position == null) {

            log.trace("Sell size not not available. Null position.");

            return ZERO;

        }

        if (Objects.equals(TRUE, context.isMarginable(key))) {

            BigDecimal limitSize = calculateFundingLimitSize(context, request, price);

            BigDecimal longPosition = position.max(ZERO);

            BigDecimal netSize = limitSize.add(longPosition).max(ZERO);

            log.trace("Margin sell size : {} (position=[{}] funding=[{}])", netSize, position, limitSize);

            return netSize;

        }

        BigDecimal exposure = ofNullable(request.getTradingExposure()).orElse(ZERO);

        BigDecimal exposed = position.multiply(exposure);

        BigDecimal rounded = context.roundLotSize(key, exposed, DOWN);

        log.trace("Cash sell size : {} (position=[{}] exposure=[{}])", rounded, position, exposure);

        return ofNullable(rounded).orElse(ZERO);

    }

}
