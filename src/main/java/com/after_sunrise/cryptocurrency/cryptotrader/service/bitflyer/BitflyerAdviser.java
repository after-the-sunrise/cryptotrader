package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateAdviser;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ProductType.*;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.UP;
import static java.util.Optional.ofNullable;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class BitflyerAdviser extends TemplateAdviser implements BitflyerService {

    private static final double SWAP_RATE = 0.0004;

    private static final Map<ProductType, ProductType> UNDERLIERS = new EnumMap<>(ProductType.class);

    private static final Set<ProductType> HEDGE_RAW = EnumSet.of(BTC_JPY, BTCJPY_MAT1WK, BTCJPY_MAT2WK, COLLATERAL_BTC);

    private static final Set<ProductType> HEDGE_CCY = EnumSet.of(ETH_BTC, BCH_BTC);

    private static final Map<ProductType, Set<ProductType>> HEDGES = new EnumMap<>(ProductType.class);

    static {
        UNDERLIERS.put(BTCJPY_MAT1WK, BTC_JPY);
        UNDERLIERS.put(BTCJPY_MAT2WK, BTC_JPY);
        HEDGES.put(FX_BTC_JPY, EnumSet.copyOf(CollectionUtils.union(HEDGE_RAW, HEDGE_CCY)));
    }

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

        ProductType underlier = UNDERLIERS.get(product);

        if (underlier == null) {
            return null;
        }

        return Key.build(Key.from(request)).instrument(underlier.name()).build();

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

    @VisibleForTesting
    BigDecimal getEquivalentSize(Context context, Request request, ProductType type) {

        Key key = Key.build(Key.from(request)).instrument(type.name()).build();

        BigDecimal position = context.getInstrumentPosition(key);

        if (position == null) {
            return null;
        }

        if (position.signum() == 0) {
            return ZERO;
        }

        if (HEDGE_CCY.contains(type)) {

            BigDecimal price = context.getMidPrice(key);

            if (price == null) {
                price = context.getLastPrice(key);
            }

            if (price == null) {
                return null;
            }

            position = position.multiply(price);

        }

        return position;

    }

    @VisibleForTesting
    BigDecimal getHedgeSize(Context context, Request request, Set<ProductType> products) {

        BigDecimal hedged = context.getInstrumentPosition(Key.from(request));

        if (hedged == null) {
            return null;
        }

        BigDecimal outright = ZERO;

        for (ProductType type : products) {

            BigDecimal position = getEquivalentSize(context, request, type);

            if (position == null) {
                return null;
            }

            outright = outright.add(ofNullable(position).orElse(ZERO));

        }

        return outright.add(hedged).negate();

    }

    private BigDecimal roundExposureSize(Context context, Request request, BigDecimal size) {

        BigDecimal exposed = size.multiply(request.getTradingExposure());

        BigDecimal rounded = context.roundLotSize(Key.from(request), exposed, UP);

        if (rounded != null && size.compareTo(rounded) < 0) {
            return ZERO;
        }

        return rounded;

    }

    @Override
    protected BigDecimal adjustBuyLimitSize(Context context, Request request, BigDecimal size) {

        Set<ProductType> hedgeProducts = HEDGES.get(ProductType.find(request.getInstrument()));

        if (CollectionUtils.isEmpty(hedgeProducts)) {
            return size;
        }

        BigDecimal hedgeSize = getHedgeSize(context, request, hedgeProducts);

        if (hedgeSize == null) {
            return ZERO;
        }

        return roundExposureSize(context, request, hedgeSize.max(ZERO));

    }


    @Override
    protected BigDecimal adjustSellLimitSize(Context context, Request request, BigDecimal size) {

        Set<ProductType> hedgeProducts = HEDGES.get(ProductType.find(request.getInstrument()));

        if (CollectionUtils.isEmpty(hedgeProducts)) {
            return size;
        }

        BigDecimal hedgeSize = getHedgeSize(context, request, hedgeProducts);

        if (hedgeSize == null) {
            return ZERO;
        }

        return roundExposureSize(context, request, hedgeSize.min(ZERO).abs());

    }

}
