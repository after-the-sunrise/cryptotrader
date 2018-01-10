package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.*;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.impl.AbstractService;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections.MapUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.DoubleStream;

import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.*;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.*;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ONE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class TemplateAdviser extends AbstractService implements Adviser {

    static final int SIGNUM_BUY = 1;

    static final int SIGNUM_SELL = -1;

    static final int SAMPLES = 5;

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

        BigDecimal basis = calculateBasis(context, request, estimation);

        BigDecimal bBasis = calculateBuyBasis(context, request, basis);

        BigDecimal sBasis = calculateSellBasis(context, request, basis);

        BigDecimal bPrice = calculateBuyLimitPrice(context, request, weighedPrice, bBasis);

        BigDecimal sPrice = calculateSellLimitPrice(context, request, weighedPrice, sBasis);

        BigDecimal bSize = calculateBuyLimitSize(context, request, bPrice);

        BigDecimal sSize = calculateSellLimitSize(context, request, sPrice);

        Advice advice = Advice.builder().buyLimitPrice(bPrice).buyLimitSize(bSize).buySpread(bBasis) //
                .sellLimitPrice(sPrice).sellLimitSize(sSize).sellSpread(sBasis).build();

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
    BigDecimal calculateBasis(Context context, Request request, Estimation estimation) {

        Key key = Key.from(request);

        BigDecimal commission = context.getCommissionRate(key);

        if (commission == null) {

            log.trace("Basis not available. Null commission.");

            return null;
        }

        BigDecimal staticBasis = request.getTradingSpread();

        if (staticBasis == null) {

            log.trace("Basis not available. Null spread.");

            return null;
        }

        BigDecimal dynamicBasis = ofNullable(calculateDeviation(context, request)).orElse(staticBasis);

        BigDecimal basis = staticBasis.max(dynamicBasis).add(commission);

        BigDecimal confidence = trimToZero(estimation.getConfidence()).min(ONE).max(ZERO);

        BigDecimal confidenceBasis = basis.multiply(ONE.add(ONE.subtract(confidence)));

        BigDecimal adjustedBasis = adjustBasis(context, request, confidenceBasis);

        log.trace("Basis : {} (static=[{}] dynamic=[{}] commission=[{}])",
                adjustedBasis, staticBasis, dynamicBasis, commission);

        return adjustedBasis;

    }

    protected BigDecimal adjustBasis(Context context, Request request, BigDecimal basis) {
        return basis;
    }

    @VisibleForTesting
    BigDecimal calculateDeviation(Context context, Request request) {

        BigDecimal sigma = request.getTradingSigma();

        if (sigma.signum() <= 0) {
            return ZERO;
        }

        Integer samples = trim(request.getTradingSamples(), 0);

        double highest = 0.0;

        while (samples >= SAMPLES) {

            Duration interval = Duration.between(request.getCurrentTime(), request.getTargetTime());

            Instant to = request.getCurrentTime();

            Instant from = to.minus(interval.toMillis() * samples, MILLIS);

            List<Trade> trades = context.listTrades(Key.from(request), from.minus(interval));

            NavigableMap<Instant, BigDecimal> prices = collapsePrices(trades, interval, from, to);

            NavigableMap<Instant, BigDecimal> returns = calculateReturns(prices);

            double[] doubles = returns.values().stream().filter(Objects::nonNull)
                    .mapToDouble(BigDecimal::doubleValue).toArray();

            double average = DoubleStream.of(doubles).average().orElse(Double.NaN);

            double variance = DoubleStream.of(doubles).map(d -> Math.pow(d - average, 2)).sum() / (doubles.length - 1);

            double deviation = Math.sqrt(variance) * sigma.doubleValue() + Math.abs(average);

            highest = Double.isFinite(deviation) ? Math.max(highest, deviation) : highest;

            samples = samples / 2;

        }

        return BigDecimal.valueOf(highest).setScale(SCALE, HALF_UP);

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

        BigDecimal offset = calculateFundingOffset(context, request);

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

    @VisibleForTesting
    BigDecimal calculateFundingOffset(Context context, Request request) {

        BigDecimal offset = request.getFundingOffset();

        Key key = Key.from(request);

        BigDecimal basePrice = context.getMidPrice(key);

        if (basePrice == null || basePrice.signum() == 0) {
            return offset;
        }

        Map<String, Set<String>> offsetProducts = request.getFundingMultiplierProducts();

        BigDecimal compositePrice = calculateComposite(offsetProducts, (site, product) -> {

            Key offsetKey = Key.build(key).site(site).instrument(product).build();

            return context.getMidPrice(offsetKey);

        });

        if (compositePrice == null || compositePrice.signum() == 0) {
            return offset;
        }

        BigDecimal adjustment = compositePrice.divide(basePrice, SCALE, HALF_UP).subtract(ONE);

        BigDecimal multiplier;

        if (adjustment.signum() > 0) {
            multiplier = request.getFundingPositiveMultiplier();
        } else {
            multiplier = request.getFundingNegativeMultiplier();
        }

        BigDecimal basis = adjustment.multiply(multiplier);

        BigDecimal result = offset.add(basis.setScale(SCALE, HALF_UP));

        BigDecimal max = request.getFundingPositiveThreshold();

        if (max != null && max.signum() != 0) {
            result = result.min(max);
        }

        BigDecimal min = request.getFundingNegativeThreshold();

        if (min != null && min.signum() != 0) {
            result = result.max(min);
        }

        return result;

    }

    @VisibleForTesting
    BigDecimal calculateRecentPrice(Context context, Request request, int signum) {

        Duration duration = request.getTradingDuration();

        if (duration.isZero()) {
            return null;
        }

        Key key = Key.from(request);

        Comparator<BigDecimal> comparator = signum == SIGNUM_BUY ? Comparator.reverseOrder() : Comparator.naturalOrder();

        BigDecimal price;

        if (duration.isNegative()) {

            Instant cutoff = request.getCurrentTime().plus(duration);

            double[] average = {0.0, 0.0, 0.0};

            trimToEmpty(context.listTrades(key, cutoff)).stream()
                    .filter(Objects::nonNull)
                    .filter(t -> t.getTimestamp() != null)
                    .filter(t -> t.getTimestamp().isAfter(cutoff))
                    .filter(t -> t.getPrice() != null)
                    .filter(t -> t.getPrice().signum() != 0)
                    .filter(t -> t.getSize() != null)
                    .filter(t -> t.getSize().signum() != 0)
                    .forEach(t -> {
                        average[0] += t.getSize().multiply(t.getPrice()).doubleValue();
                        average[1] += t.getSize().doubleValue();
                        average[2] += 1;
                    });

            price = average[2] > 0 ? BigDecimal.valueOf(average[0] / average[1]).setScale(SCALE, HALF_UP) : null;

        } else {

            Instant cutoff = request.getCurrentTime().minus(duration);

            price = ofNullable(context.listExecutions(key))
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

        return price;

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
    BigDecimal calculateBuyBoundaryPrice(Context context, Request request, BigDecimal basis) {

        Key key = Key.from(request);

        BigDecimal ask0 = context.getBestAskPrice(key);

        if (ask0 == null) {
            return null;
        }

        BigDecimal ask1 = ask0.subtract(EPSILON);

        BigDecimal recent = ofNullable(calculateRecentPrice(context, request, SIGNUM_SELL))
                .map(r -> r.multiply(ONE.subtract(trimToZero(basis)).subtract(trimToZero(basis)))).orElse(ask0);

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
    BigDecimal calculateSellBoundaryPrice(Context context, Request request, BigDecimal basis) {

        Key key = Key.from(request);

        BigDecimal bid0 = context.getBestBidPrice(key);

        if (bid0 == null) {
            return null;
        }

        BigDecimal bid1 = bid0.add(EPSILON);

        BigDecimal recent = ofNullable(calculateRecentPrice(context, request, SIGNUM_BUY))
                .map(r -> r.multiply(ONE.add(trimToZero(basis)).add(trimToZero(basis)))).orElse(bid0);

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

        BigDecimal bound = calculateBuyBoundaryPrice(context, request, basis);

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

        BigDecimal bound = calculateSellBoundaryPrice(context, request, basis);

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
    BigDecimal calculateTradingExposure(Context context, Request request) {

        BigDecimal exposure = ofNullable(request.getTradingExposure()).orElse(ZERO);

        BigDecimal offset = calculateFundingOffset(context, request);

        if (offset == null) {
            return null;
        }

        BigDecimal adjustment = ONE.add(offset);

        if (adjustment.signum() <= 0) {
            return ONE;
        }

        BigDecimal root = BigDecimal.valueOf(Math.sqrt(adjustment.doubleValue()));

        return exposure.divide(root, SCALE, HALF_UP).min(ONE);

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

        BigDecimal offset = calculateFundingOffset(context, request);

        BigDecimal adjFund = fund.multiply(ONE.add(ofNullable(offset).orElse(ZERO))).max(ZERO);

        BigDecimal product = adjFund.divide(price, SCALE, HALF_UP);

        BigDecimal exposure = ofNullable(calculateTradingExposure(context, request)).orElse(ZERO);

        BigDecimal commission = ofNullable(context.getCommissionRate(key)).orElse(ZERO).max(ZERO).min(ONE);

        BigDecimal available = fund.multiply(ONE.subtract(commission)).divide(price, SCALE, DOWN);

        BigDecimal exposed = product.multiply(exposure).min(available);

        log.trace("Funding exposure size : {} (fund=[{}] price=[{}])", exposed, adjFund, price);

        return exposed;

    }

    @VisibleForTesting
    BigDecimal calculateInstrumentExposureSize(Context context, Request request) {

        Key key = Key.from(request);

        CurrencyType currency = context.getInstrumentCurrency(key);

        BigDecimal basePrice = context.getConversionPrice(key, currency);

        if (basePrice == null) {

            log.trace("No base price for {}:{}:{}", key.getSite(), key.getInstrument(), currency);

            return ZERO;

        }

        Map<String, Set<String>> hedgeProducts = ofNullable(request.getHedgeProducts())
                .filter(MapUtils::isNotEmpty)
                .orElseGet(() -> singletonMap(request.getSite(), singleton(request.getInstrument())));

        BigDecimal position = ZERO;

        for (Map.Entry<String, Set<String>> entry : hedgeProducts.entrySet()) {

            String site = entry.getKey();

            for (String instrument : entry.getValue()) {

                Key instrumentKey = Key.build(key).site(site).instrument(instrument).build();

                BigDecimal conversionPrice = context.getConversionPrice(instrumentKey, currency);

                if (conversionPrice == null) {

                    log.trace("No conversion price for {}:{}:{}", site, instrument, currency);

                    return ZERO;

                }

                BigDecimal conversionPosition = context.getInstrumentPosition(instrumentKey);

                if (conversionPosition == null) {

                    log.trace("No conversion position for {}:{}", site, instrument);

                    return ZERO;

                }

                BigDecimal basePosition = conversionPosition.divide(conversionPrice, SCALE, HALF_UP);

                position = position.add(basePosition);

            }

        }

        BigDecimal exposure = MapUtils.isNotEmpty(request.getHedgeProducts()) ? ONE :
                ofNullable(calculateTradingExposure(context, request)).orElse(ZERO);

        BigDecimal exposed = position.multiply(basePrice).multiply(exposure);

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

        BigDecimal rounded = ofNullable(context.roundLotSize(Key.from(request), size, HALF_UP)).orElse(ZERO);

        BigDecimal minimum = request.getTradingMinimum();

        if (minimum != null && rounded.compareTo(minimum) < 0) {
            rounded = ZERO;
        }

        log.trace("Buy size : {} (funding=[{}] instrument[{}])", rounded, fundingSize, instrumentSize);

        return rounded;

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

        BigDecimal rounded = ofNullable(context.roundLotSize(Key.from(request), size, HALF_UP)).orElse(ZERO);

        BigDecimal minimum = request.getTradingMinimum();

        if (minimum != null && rounded.compareTo(minimum) < 0) {
            rounded = ZERO;
        }

        log.trace("Sell size : {} (funding=[{}] instrument[{}])", rounded, fundingSize, instrumentSize);

        return rounded;

    }

}
