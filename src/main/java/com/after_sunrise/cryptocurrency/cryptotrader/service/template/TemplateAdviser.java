package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.stream.DoubleStream;

import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ONE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class TemplateAdviser implements Adviser {

    static final BigDecimal EPSILON = ONE.movePointLeft(SCALE);

    static final int SIGNUM_BUY = 1;

    static final int SIGNUM_SELL = -1;

    static final int SAMPLES = 60;

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

        BigDecimal staticBasis = spread.add(comm);

        BigDecimal dynamicBasis = ofNullable(calculateDeviation(context, request)).orElse(staticBasis);

        BigDecimal adjustedBasis = adjustBasis(context, request, staticBasis.max(dynamicBasis));

        log.trace("Basis : {} (static=[{}] dynamic=[{}])", adjustedBasis, staticBasis, dynamicBasis);

        return adjustedBasis;

    }

    @VisibleForTesting
    BigDecimal calculateDeviation(Context context, Request request) {

        BigDecimal sigma = request.getTradingSigma();

        if (sigma.signum() == 0) {
            return ZERO;
        }

        Duration interval = Duration.between(request.getCurrentTime(), request.getTargetTime());

        Instant to = request.getCurrentTime();

        Instant from = to.minus(interval.toMillis() * SAMPLES, MILLIS);

        List<Trade> trades = context.listTrades(Key.from(request), from.minus(interval));

        NavigableMap<Instant, BigDecimal> prices = collapsePrices(trades, interval, from, to);

        NavigableMap<Instant, BigDecimal> returns = calculateReturns(prices);

        double[] doubles = returns.values().stream().filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue).toArray();

        double average = DoubleStream.of(doubles).average().orElse(Double.NaN);

        double variance = DoubleStream.of(doubles).map(d -> Math.pow(d - average, 2)).sum() / (doubles.length - 1);

        double deviation = Math.sqrt(variance) * sigma.doubleValue() + Math.abs(average);

        return Double.isFinite(deviation) ? BigDecimal.valueOf(deviation).setScale(SCALE, HALF_UP) : null;

    }

    protected BigDecimal adjustBasis(Context context, Request request, BigDecimal basis) {
        return basis;
    }

    @VisibleForTesting
    BigDecimal calculatePositionRatio(Context context, Request request) {

        BigDecimal aversion = ofNullable(request.getTradingAversion()).orElse(ONE);

        if (aversion.signum() == 0) {
            return ZERO;
        }

        Key key = Key.from(request);

        BigDecimal mid = context.getMidPrice(key);

        BigDecimal funding = context.getFundingPosition(key);

        BigDecimal structure = context.getInstrumentPosition(key);

        if (mid == null || funding == null || structure == null) {

            log.trace("Position ratio unavailable : price=[{}] funding=[{}] structure=[{}]", mid, funding, structure);

            return null;

        }

        BigDecimal offset = adjustFundingOffset(context, request, request.getFundingOffset());

        BigDecimal adjFunding = funding.multiply(ONE.add(ofNullable(offset).orElse(ZERO))).max(ZERO);

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

        BigDecimal aversionRatio = ratio.multiply(aversion).setScale(SCALE, HALF_UP);

        log.trace("Position ratio: {} (ratio=[{}], fund=[{}], structure=[{}] price=[{}])",
                aversionRatio, ratio, adjFunding, structure, mid);

        return aversionRatio;

    }

    protected BigDecimal adjustFundingOffset(Context context, Request request, BigDecimal offset) {
        return offset;
    }

    @VisibleForTesting
    BigDecimal calculateRecentPrice(Context context, Request request, int signum) {

        Instant cutoff = request.getCurrentTime().minus(request.getTradingDuration());

        if (cutoff.compareTo(request.getCurrentTime()) >= 0) {
            return null;
        }

        Key key = Key.from(request);

        Comparator<BigDecimal> comparator = signum == SIGNUM_BUY ? Comparator.reverseOrder() : Comparator.naturalOrder();

        return ofNullable(context.listExecutions(key))
                .orElse(emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(v -> v.getTime() != null)
                .filter(v -> v.getTime().isAfter(cutoff))
                .filter(v -> v.getPrice() != null)
                .filter(v -> v.getPrice().signum() != 0)
                .filter(v -> v.getSize() != null)
                .filter(v -> v.getSize().signum() == signum)
                .map(Order.Execution::getPrice)
                .sorted(comparator)
                .findFirst().orElse(null);

    }

    @VisibleForTesting
    BigDecimal calculateBuyLossBasis(Context context, Request request) {

        BigDecimal market = context.getBestBidPrice(Key.from(request));

        if (market == null) {
            return ZERO;
        }

        BigDecimal latest = calculateRecentPrice(context, request, SIGNUM_BUY);

        if (latest == null || latest.signum() == 0) {
            return ZERO;
        }

        BigDecimal lossPrice = latest.subtract(market).max(ZERO);

        BigDecimal lossBasis = lossPrice.divide(latest, SCALE, ROUND_UP);

        BigDecimal aversion = ofNullable(request.getTradingAversion()).orElse(ONE);

        return lossBasis.multiply(aversion).max(ZERO);

    }

    @VisibleForTesting
    BigDecimal calculateBuyBasis(Context context, Request request, BigDecimal base) {

        if (base == null) {
            return null;
        }

        BigDecimal additional = ofNullable(request.getTradingSpreadBid()).orElse(ZERO);

        BigDecimal positionRatio = ofNullable(calculatePositionRatio(context, request)).orElse(ZERO).max(ZERO);

        BigDecimal positionBase = base.add(additional).multiply(ONE.add(positionRatio));

        BigDecimal lossBasis = ofNullable(calculateBuyLossBasis(context, request)).orElse(ZERO);

        BigDecimal adjusted = adjustBuyBasis(context, request, positionBase.add(lossBasis));

        log.trace("Buy Basis : {} (additional=[{}] position=[{}] loss=[{}])",
                adjusted, additional, positionRatio, lossBasis);

        return adjusted;

    }

    protected BigDecimal adjustBuyBasis(Context context, Request request, BigDecimal basis) {
        return basis;
    }

    @VisibleForTesting
    BigDecimal calculateSellLossBasis(Context context, Request request) {

        BigDecimal market = context.getBestAskPrice(Key.from(request));

        if (market == null) {
            return ZERO;
        }

        BigDecimal latest = calculateRecentPrice(context, request, SIGNUM_SELL);

        if (latest == null || latest.signum() == 0) {
            return ZERO;
        }

        BigDecimal lossPrice = market.subtract(latest).max(ZERO);

        BigDecimal lossBasis = lossPrice.divide(latest, SCALE, ROUND_UP);

        BigDecimal aversion = ofNullable(request.getTradingAversion()).orElse(ONE);

        return lossBasis.multiply(aversion).max(ZERO);

    }

    @VisibleForTesting
    BigDecimal calculateSellBasis(Context context, Request request, BigDecimal base) {

        if (base == null) {
            return null;
        }

        BigDecimal additional = ofNullable(request.getTradingSpreadAsk()).orElse(ZERO);

        BigDecimal positionRatio = ofNullable(calculatePositionRatio(context, request)).orElse(ZERO).min(ZERO).abs();

        BigDecimal positionBase = base.add(additional).multiply(ONE.add(positionRatio));

        BigDecimal lossBasis = ofNullable(calculateSellLossBasis(context, request)).orElse(ZERO);

        BigDecimal adjusted = adjustSellBasis(context, request, positionBase.add(lossBasis));

        log.trace("Sell Basis : {} (additional=[{}] position=[{}] loss=[{}])",
                adjusted, additional, positionRatio, lossBasis);

        return adjusted;

    }

    protected BigDecimal adjustSellBasis(Context context, Request request, BigDecimal basis) {
        return basis;
    }

    @VisibleForTesting
    BigDecimal calculateBuyBoundaryPrice(Context context, Request request) {

        Key key = Key.from(request);

        BigDecimal ask0 = context.getBestAskPrice(key);

        if (ask0 == null) {
            return null;
        }

        BigDecimal ask1 = ask0.subtract(EPSILON);

        BigDecimal basis = ofNullable(calculateBasis(context, request)).orElse(ZERO);

        BigDecimal recent = ofNullable(calculateRecentPrice(context, request, SIGNUM_SELL))
                .map(r -> r.multiply(ONE.subtract(basis).subtract(basis))).orElse(ask0);

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

        BigDecimal price = ask1.min(bid1).min(recent);

        return adjustBuyBoundaryPrice(context, request, context.roundTickSize(key, price, DOWN));

    }

    protected BigDecimal adjustBuyBoundaryPrice(Context context, Request request, BigDecimal price) {
        return price;
    }

    @VisibleForTesting
    BigDecimal calculateSellBoundaryPrice(Context context, Request request) {

        Key key = Key.from(request);

        BigDecimal bid0 = context.getBestBidPrice(key);

        if (bid0 == null) {
            return null;
        }

        BigDecimal bid1 = bid0.add(EPSILON);

        BigDecimal basis = ofNullable(calculateBasis(context, request)).orElse(ZERO);

        BigDecimal recent = ofNullable(calculateRecentPrice(context, request, SIGNUM_BUY))
                .map(r -> r.multiply(ONE.add(basis).add(basis))).orElse(bid0);

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

        BigDecimal price = bid1.max(ask1).max(recent);

        return adjustSellBoundaryPrice(context, request, context.roundTickSize(key, price, UP));

    }

    protected BigDecimal adjustSellBoundaryPrice(Context context, Request request, BigDecimal price) {
        return price;
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

        BigDecimal basisPrice = weighedPrice.multiply(ONE.subtract(basis));

        BigDecimal boundPrice = basisPrice.min(bound);

        BigDecimal rounded = context.roundTickSize(key, boundPrice, DOWN);

        log.trace("Buy price : {} (target=[{}] basis=[{}])", rounded, boundPrice, basisPrice);

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

        BigDecimal basisPrice = weighedPrice.multiply(ONE.add(basis));

        BigDecimal boundPrice = basisPrice.max(bound);

        BigDecimal rounded = context.roundTickSize(key, boundPrice, UP);

        log.trace("Sell price : {} (target=[{}] basis=[{}])", rounded, boundPrice, basisPrice);

        return rounded;

    }

    @VisibleForTesting
    BigDecimal calculateFundingExposureSize(Context context, Request request, BigDecimal price) {

        if (price == null || price.signum() == 0) {

            log.trace("No funding exposure size. Price : {}", price);

            return ZERO;

        }

        Key key = Key.from(request);

        BigDecimal fund = context.getFundingPosition(key);

        if (fund == null) {

            log.trace("No funding exposure size. Null funding position.");

            return ZERO;

        }

        BigDecimal offset = adjustFundingOffset(context, request, request.getFundingOffset());

        BigDecimal adjFund = fund.multiply(ONE.add(ofNullable(offset).orElse(ZERO))).max(ZERO);

        BigDecimal product = adjFund.divide(price, SCALE, HALF_UP);

        BigDecimal exposure = ofNullable(request.getTradingExposure()).orElse(ZERO);

        BigDecimal exposed = product.multiply(exposure).min(fund.divide(price, SCALE, DOWN));

        log.trace("Funding exposure size : {} (fund=[{}] price=[{}])", exposed, adjFund, price);

        return exposed;

    }

    @VisibleForTesting
    BigDecimal calculateInstrumentExposureSize(Context context, Request request) {

        Key key = Key.from(request);

        BigDecimal position = context.getInstrumentPosition(key);

        if (position == null) {

            log.trace("No instrument exposure size. Null instrument position.");

            return ZERO;

        }

        BigDecimal exposure = ofNullable(request.getTradingExposure()).orElse(ZERO);

        BigDecimal exposed = position.multiply(exposure);

        log.trace("Instrument exposure size : {} (position=[{}])", exposed, position);

        return exposed;

    }

    @VisibleForTesting
    BigDecimal calculateBuyLimitSize(Context context, Request request, BigDecimal price) {

        BigDecimal fundingSize = calculateFundingExposureSize(context, request, price);

        BigDecimal instrumentSize = calculateInstrumentExposureSize(context, request);

        BigDecimal size;

        if (Objects.equals(TRUE, context.isMarginable(Key.from(request)))) {

            size = fundingSize.subtract(instrumentSize).max(ZERO).multiply(HALF);

        } else {

            BigDecimal excess = instrumentSize.subtract(fundingSize).max(ZERO).movePointLeft(INTEGER_ONE);

            size = fundingSize.subtract(excess).max(ZERO);

        }

        BigDecimal rounded = context.roundLotSize(Key.from(request), size, HALF_UP);

        log.trace("Buy size : {} (funding=[{}] instrument[{}])", rounded, fundingSize, instrumentSize);

        return adjustBuyLimitSize(context, request, ofNullable(rounded).orElse(ZERO));

    }

    protected BigDecimal adjustBuyLimitSize(Context context, Request request, BigDecimal size) {
        return size;
    }

    @VisibleForTesting
    BigDecimal calculateSellLimitSize(Context context, Request request, BigDecimal price) {

        BigDecimal instrumentSize = calculateInstrumentExposureSize(context, request);

        BigDecimal fundingSize = calculateFundingExposureSize(context, request, price);

        BigDecimal size;

        if (Objects.equals(TRUE, context.isMarginable(Key.from(request)))) {

            size = fundingSize.add(instrumentSize).max(ZERO).multiply(HALF);

        } else {

            BigDecimal excess = fundingSize.subtract(instrumentSize).max(ZERO).movePointLeft(INTEGER_ONE);

            size = instrumentSize.subtract(excess).max(ZERO);

        }

        BigDecimal rounded = context.roundLotSize(Key.from(request), size, HALF_UP);

        log.trace("Sell size : {} (funding=[{}] instrument[{}])", rounded, fundingSize, instrumentSize);

        return adjustSellLimitSize(context, request, ofNullable(rounded).orElse(ZERO));

    }

    protected BigDecimal adjustSellLimitSize(Context context, Request request, BigDecimal size) {
        return size;
    }

}
