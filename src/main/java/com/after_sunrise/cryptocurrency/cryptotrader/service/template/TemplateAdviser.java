package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class TemplateAdviser implements Adviser {

    private static final BigDecimal EPSILON = ONE.movePointLeft(SCALE);

    static final int SIGNUM_BUY = 1;

    static final int SIGNUM_SELL = -1;

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

        BigDecimal bBasis = calculateBuyBasis(context, request, basis);

        BigDecimal sBasis = calculateSellBasis(context, request, basis);

        BigDecimal bPrice = calculateBuyLimitPrice(context, request, weighedPrice, bBasis);

        BigDecimal sPrice = calculateSellLimitPrice(context, request, weighedPrice, sBasis);

        BigDecimal bSize = calculateBuyLimitSize(context, request, bPrice);

        BigDecimal sSize = calculateSellLimitSize(context, request, sPrice);

        Advice advice = Advice.builder().buyLimitPrice(bPrice).buyLimitSize(bSize) //
                .sellLimitPrice(sPrice).sellLimitSize(sSize).build();

        log.trace("Advice : {} - {}", advice, request);

        return advice;

    }

    protected BigDecimal calculateAdditionalBasis(Context context, Request request) {
        return ZERO;
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

        BigDecimal additional = ofNullable(calculateAdditionalBasis(context, request)).orElse(ZERO);

        return spread.add(comm).add(additional);

    }

    @VisibleForTesting
    BigDecimal calculateRecentPrice(Context context, Request request, int signum) {

        Instant cutoff = request.getCurrentTime().minus(request.getTradingDuration());

        Key key = Key.from(request);

        List<Order.Execution> executions = ofNullable(context.listExecutions(key))
                .orElse(emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(v -> v.getTime() != null)
                .filter(v -> v.getTime().isAfter(cutoff))
                .filter(v -> v.getPrice() != null)
                .filter(v -> v.getPrice().signum() != 0)
                .filter(v -> v.getSize() != null)
                .filter(v -> v.getSize().signum() == signum)
                .sorted(comparing(Order.Execution::getTime).reversed())
                .collect(Collectors.toList());

        double numerator = 0;
        double denominator = 0;

        for (Order.Execution exec : executions) {

            double price = exec.getPrice().doubleValue();

            double size = exec.getSize().doubleValue();

            double weight = exec.getTime().toEpochMilli() - cutoff.toEpochMilli();

            numerator += price * size * weight;

            denominator += size * weight;

        }

        if (denominator == 0.0) {
            return null;
        }

        return BigDecimal.valueOf(numerator / denominator).setScale(SCALE, HALF_UP);

    }

    @VisibleForTesting
    BigDecimal calculateBuyBasis(Context context, Request request, BigDecimal base) {

        if (base == null) {
            return null;
        }

        BigDecimal market = context.getBestBidPrice(Key.from(request));

        if (market == null) {
            return base;
        }

        BigDecimal latest = calculateRecentPrice(context, request, SIGNUM_BUY);

        if (latest == null || latest.signum() == 0) {
            return base;
        }

        BigDecimal lossPrice = latest.subtract(market).max(ZERO);

        BigDecimal lossRatio = lossPrice.divide(latest, SCALE, ROUND_UP);

        return base.max(lossRatio);

    }

    @VisibleForTesting
    BigDecimal calculateSellBasis(Context context, Request request, BigDecimal base) {

        if (base == null) {
            return null;
        }

        BigDecimal market = context.getBestAskPrice(Key.from(request));

        if (market == null) {
            return base;
        }

        BigDecimal latest = calculateRecentPrice(context, request, SIGNUM_SELL);

        if (latest == null || latest.signum() == 0) {
            return base;
        }

        BigDecimal lossPrice = market.subtract(latest).max(ZERO);

        BigDecimal lossRatio = lossPrice.divide(latest, SCALE, ROUND_UP);

        return base.max(lossRatio);

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

        BigDecimal offset = ofNullable(request.getFundingOffset()).orElse(ZERO);

        BigDecimal adjFunding = funding.multiply(ONE.add(offset));

        BigDecimal equivalent = structure.multiply(mid);

        BigDecimal ratio;

        if (Objects.equals(TRUE, context.isMarginable(key))) {

            // = Equivalent / (Funding / 2)
            // = 2 * Equivalent / Funding
            // (Funding / 2 = Funding for single side)

            if (adjFunding.signum() == 0) {
                return ZERO;
            }

            // Leveraged short can be larger than the funding.
            ratio = equivalent.add(equivalent).divide(adjFunding, SCALE, HALF_UP);

        } else {

            // = Diff / Average
            // = (X - Y) / [(X + Y) / 2]
            // = 2 * (X - Y) / (X + Y)

            BigDecimal sum = equivalent.add(adjFunding);

            if (sum.signum() == 0) {
                return ZERO;
            }

            BigDecimal diff = equivalent.subtract(adjFunding);

            ratio = diff.add(diff).divide(sum, SCALE, HALF_UP);

        }

        log.trace("Position ratio: {} (fund=[{}], structure=[{}] price=[{}])", ratio, adjFunding, structure, mid);

        return ratio;

    }

    @VisibleForTesting
    BigDecimal calculateBuyBoundaryPrice(Context context, Request request) {

        Key key = Key.from(request);

        BigDecimal ask0 = context.getBestAskPrice(key);

        if (ask0 == null) {
            return null;
        }

        BigDecimal ask1 = context.roundTickSize(key, ask0.subtract(EPSILON), DOWN);

        if (ask1 == null) {
            return null;
        }

        BigDecimal recent = ofNullable(calculateRecentPrice(context, request, SIGNUM_SELL)).orElse(ask0);

        BigDecimal bid0 = ofNullable(context.getBestBidPrice(key)).orElse(ask0);

        BigDecimal bid1 = bid0;

        if (ofNullable(context.listActiveOrders(key)).orElse(emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(o -> o.getOrderQuantity() != null)
                .filter(o -> o.getOrderQuantity().signum() == SIGNUM_BUY)
                .filter(o -> o.getOrderPrice() != null)
                .filter(o -> o.getOrderPrice().compareTo(bid0) == 0)
                .count() == 0) {

            bid1 = ofNullable(context.roundTickSize(key, bid0.add(EPSILON), UP)).orElse(bid0);

        }

        return ask1.min(bid1).min(recent);

    }

    @VisibleForTesting
    BigDecimal calculateSellBoundaryPrice(Context context, Request request) {

        Key key = Key.from(request);

        BigDecimal bid0 = context.getBestBidPrice(key);

        if (bid0 == null) {
            return null;
        }

        BigDecimal bid1 = context.roundTickSize(key, bid0.add(EPSILON), UP);

        if (bid1 == null) {
            return null;
        }

        BigDecimal recent = ofNullable(calculateRecentPrice(context, request, SIGNUM_BUY)).orElse(bid0);

        BigDecimal ask0 = ofNullable(context.getBestAskPrice(key)).orElse(bid0);

        BigDecimal ask1 = ask0;

        if (ofNullable(context.listActiveOrders(key)).orElse(emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(o -> o.getOrderQuantity() != null)
                .filter(o -> o.getOrderQuantity().signum() == SIGNUM_SELL)
                .filter(o -> o.getOrderPrice() != null)
                .filter(o -> o.getOrderPrice().compareTo(ask0) == 0)
                .count() == 0) {

            ask1 = ofNullable(context.roundTickSize(key, ask0.subtract(EPSILON), DOWN)).orElse(ask0);

        }

        return bid1.max(ask1).max(recent);

    }

    @VisibleForTesting
    BigDecimal calculateBuyLimitPrice(Context context, Request request, BigDecimal weighedPrice, BigDecimal basis) {

        if (weighedPrice == null || basis == null) {

            log.trace("Buy price not available : weighed=[{}] basis=[{}]", weighedPrice, basis);

            return null;

        }

        Key key = Key.from(request);

        BigDecimal bound = calculateBuyBoundaryPrice(context, request);

        if (bound == null) {

            log.trace("Buy price not available : No bound price.");

            return null;

        }

        BigDecimal ratio = calculatePositionRatio(context, request);

        if (ratio == null) {

            log.trace("Buy price not available : No position ratio.");

            return null;

        }

        BigDecimal ratioBasis = basis.multiply(ONE.add(ratio.max(ZERO)));

        BigDecimal ratioPrice = weighedPrice.multiply(ONE.subtract(ratioBasis));

        BigDecimal boundPrice = ratioPrice.min(bound);

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

        BigDecimal bound = calculateSellBoundaryPrice(context, request);

        if (bound == null) {

            log.trace("Sell price not available : No bound price.");

            return null;

        }

        BigDecimal ratio = calculatePositionRatio(context, request);

        if (ratio == null) {

            log.trace("Sell price not available : No position ratio.");

            return null;

        }

        BigDecimal ratioBasis = basis.multiply(ONE.add(ratio.min(ZERO).abs()));

        BigDecimal ratioPrice = weighedPrice.multiply(ONE.add(ratioBasis));

        BigDecimal boundPrice = ratioPrice.max(bound);

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

        BigDecimal offset = ofNullable(request.getFundingOffset()).orElse(ZERO);

        BigDecimal adjFund = fund.multiply(ONE.add(offset));

        BigDecimal product = adjFund.divide(price, SCALE, DOWN);

        BigDecimal exposure = ofNullable(request.getTradingExposure()).orElse(ZERO);

        BigDecimal exposed = product.multiply(exposure);

        BigDecimal rounded = context.roundLotSize(key, exposed, DOWN);

        log.trace("Funding limit size : {} (exposure=[{}] fund=[{}] price=[{}])", rounded, exposure, adjFund, price);

        return ofNullable(rounded).orElse(ZERO);

    }

    protected BigDecimal calculateExposedInstrumentPosition(Context context, Request request) {

        Key key = Key.from(request);

        return context.getInstrumentPosition(key);

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

        BigDecimal position = calculateExposedInstrumentPosition(context, request);

        if (position == null) {

            log.trace("Margin buy size not not available. Null position.");

            return ZERO;

        }

        BigDecimal shortPosition = position.min(ZERO).abs();

        BigDecimal netSize = limitSize.max(shortPosition);

        BigDecimal rounded = context.roundLotSize(key, netSize, DOWN);

        log.trace("Margin Buy size : {} (position=[{}] funding=[{}])", rounded, position, limitSize);

        return rounded;

    }

    @VisibleForTesting
    BigDecimal calculateSellLimitSize(Context context, Request request, BigDecimal price) {

        Key key = Key.from(request);

        BigDecimal position = calculateExposedInstrumentPosition(context, request);

        if (position == null) {

            log.trace("Sell size not not available. Null position.");

            return ZERO;

        }

        if (!Objects.equals(TRUE, context.isMarginable(key))) {

            BigDecimal exposure = ofNullable(request.getTradingExposure()).orElse(ZERO);

            BigDecimal exposed = position.multiply(exposure);

            BigDecimal rounded = context.roundLotSize(key, exposed, DOWN);

            log.trace("Cash sell size : {} (position=[{}] exposure=[{}])", rounded, position, exposure);

            return ofNullable(rounded).orElse(ZERO);

        }

        BigDecimal limitSize = calculateFundingLimitSize(context, request, price);

        BigDecimal longPosition = position.max(ZERO);

        BigDecimal netSize = limitSize.max(longPosition);

        BigDecimal rounded = context.roundLotSize(key, netSize, DOWN);

        log.trace("Margin sell size : {} (position=[{}] funding=[{}])", rounded, position, limitSize);

        return rounded;

    }

}
