package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateAdviser;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections.MapUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.math.RoundingMode.UP;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitflyerAdviser extends TemplateAdviser implements BitflyerService {

    private static final double SWAP_RATE = 0.0004;

    private static final BigDecimal PHYSICAL_RATE = new BigDecimal("0.20");

    public BitflyerAdviser() {
        super(ID);
    }

    @VisibleForTesting
    BigDecimal calculateSwapRate(Context context, Request request) {

        Instant now = request.getCurrentTime();

        if (now == null) {
            return ZERO;
        }

        ZonedDateTime expiry = context.getExpiry(Key.from(request));

        if (expiry == null) {
            return ZERO; // Not an expiry product.
        }

        ZonedDateTime sod = expiry.truncatedTo(ChronoUnit.DAYS);

        Duration swapFree = Duration.between(sod, expiry);

        Duration maturity = Duration.between(request.getCurrentTime(), expiry);

        if (maturity.compareTo(swapFree) < 0) {
            return ZERO; // Expiring without swap.
        }

        long swaps = maturity.toDays();

        double rate = Math.pow(1 + SWAP_RATE, swaps) - 1;

        return BigDecimal.valueOf(rate).setScale(SCALE, UP);

    }

    @Override
    protected BigDecimal adjustBasis(Context context, Request request, BigDecimal basis) {

        if (basis == null) {
            return null;
        }

        BigDecimal swapRate = calculateSwapRate(context, request);

        return basis.add(swapRate);

    }

    @VisibleForTesting
    Key getUnderlyingKey(Request request) {

        ProductType product = ProductType.find(request.getInstrument());

        if (product == null) {
            return null;
        }

        ProductType underlying = product.getUnderlying();

        if (underlying == null) {
            return null;
        }

        return Key.build(Key.from(request)).instrument(underlying.name()).build();

    }

    @Override
    protected BigDecimal adjustBuyBoundaryPrice(Context context, Request request, BigDecimal price) {

        if (price == null) {
            return null;
        }

        Key key = getUnderlyingKey(request);

        if (key == null) {
            return price;
        }

        BigDecimal bid = context.getBestBidPrice(key);

        BigDecimal comm = context.getCommissionRate(key);

        BigDecimal spread = request.getTradingSpread();

        BigDecimal swap = calculateSwapRate(context, request);

        if (bid == null || comm == null || swap == null) {
            return null;
        }

        BigDecimal theoretical = bid.multiply(ONE.subtract(comm).subtract(spread).subtract(swap));

        return price.min(theoretical);

    }

    @Override
    protected BigDecimal adjustSellBoundaryPrice(Context context, Request request, BigDecimal price) {

        if (price == null) {
            return null;
        }

        Key key = getUnderlyingKey(request);

        if (key == null) {
            return price;
        }

        BigDecimal ask = context.getBestAskPrice(key);

        BigDecimal comm = context.getCommissionRate(key);

        BigDecimal spread = request.getTradingSpread();

        BigDecimal swap = calculateSwapRate(context, request);

        if (ask == null || comm == null || swap == null) {
            return null;
        }

        BigDecimal theoretical = ask.multiply(ONE.add(comm).add(spread).add(swap));

        return price.max(theoretical);

    }

    @Override
    protected BigDecimal adjustFundingOffset(Context context, Request request, BigDecimal offset) {

        Map<String, Set<String>> offsetProducts = request.getFundingMultiplierProducts();

        if (MapUtils.isEmpty(offsetProducts)) {
            return offset;
        }

        Key key = Key.from(request);

        BigDecimal price = context.getMidPrice(key);

        if (price == null || price.signum() == 0) {
            return offset;
        }

        BigDecimal tilt = ONE;

        for (Entry<String, Set<String>> entry : offsetProducts.entrySet()) {

            for (String product : entry.getValue()) {

                Key offsetKey = Key.build(key).site(entry.getKey()).instrument(product).build();

                BigDecimal offsetPrice = context.getMidPrice(offsetKey);

                if (offsetPrice == null || offsetPrice.signum() == 0) {
                    return offset;
                }

                tilt = tilt.multiply(offsetPrice.divide(price, SCALE, HALF_UP));

            }

        }

        BigDecimal adjustment = tilt.subtract(ONE);

        BigDecimal multiplier;

        if (adjustment.signum() > 0) {
            multiplier = request.getFundingPositiveMultiplier();
        } else {
            multiplier = request.getFundingNegativeMultiplier();
        }

        BigDecimal basis = adjustment.min(PHYSICAL_RATE).max(PHYSICAL_RATE.negate()).multiply(multiplier);

        return offset.add(basis.setScale(SCALE, HALF_UP));

    }

    @Override
    protected BigDecimal calculateConversionPrice(Context context, Request request, CurrencyType currency) {

        ProductType product = ProductType.find(request.getInstrument());

        if (product == null) {
            return null;
        }

        CurrencyType structureCurrency = product.getStructure().getCurrency();

        if (structureCurrency == currency) {
            return ONE;
        }

        for (ProductType p : ProductType.values()) {

            if (p.getFunding().getCurrency() != structureCurrency) {
                continue;
            }

            if (p.getStructure().getCurrency() != currency) {
                continue;
            }

            Key key = Key.build(Key.from(request)).instrument(p.name()).build();

            BigDecimal price = context.getMidPrice(key);

            return price == null || price.signum() == 0 ? null : price;

        }

        for (ProductType p : ProductType.values()) {

            if (p.getStructure().getCurrency() != structureCurrency) {
                continue;
            }

            if (p.getFunding().getCurrency() != currency) {
                continue;
            }

            Key key = Key.build(Key.from(request)).instrument(p.name()).build();

            BigDecimal price = context.getMidPrice(key);

            return price == null || price.signum() == 0 ? null : ONE.divide(price, SCALE, HALF_UP);

        }

        return null;

    }

}
