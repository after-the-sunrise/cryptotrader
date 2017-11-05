package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateAdviser;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.math.RoundingMode.UP;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class BitflyerAdviser extends TemplateAdviser implements BitflyerService {

    private static final double SWAP_RATE = 0.0004;

    private static final BigDecimal PHYSICAL_RATE = new BigDecimal("0.20");

    private static final String KEY_MULTIPLIER = BitflyerAdviser.class.getName() + ".funding.multiplier";

    private static final String KEY_OFFSET_PRODUCTS = BitflyerAdviser.class.getName() + ".products.offset";

    private ImmutableConfiguration configuration;

    private Map<String, Entry<String, String>> offsetProductsCache;

    public BitflyerAdviser() {
        super(ID);
    }

    @Inject
    @VisibleForTesting
    void setConfiguration(ImmutableConfiguration configuration) {
        this.configuration = configuration;
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

    private Entry<String, String> findOffsetProduct(Request request) {

        Map<String, Entry<String, String>> offsetProducts = offsetProductsCache;

        if (offsetProducts == null) {

            String[] entries = StringUtils.split(configuration.getString(KEY_OFFSET_PRODUCTS), '|');

            offsetProducts = entries == null ? emptyMap() : unmodifiableMap(Stream.of(entries).map(s -> {

                String[] values = StringUtils.split(s, ":", 3);

                if (ArrayUtils.isEmpty(values) || values.length != 3) {
                    return null;
                }

                return new SimpleEntry<>(values[0], new SimpleEntry<>(values[1], values[2]));

            }).filter(Objects::nonNull).collect(Collectors.toMap(Entry::getKey, Entry::getValue)));

            offsetProductsCache = offsetProducts;

        }

        return offsetProducts.get(request.getInstrument());

    }

    @Override
    protected BigDecimal adjustFundingOffset(Context context, Request request, BigDecimal offset) {

        Entry<String, String> offsetProduct = findOffsetProduct(request);

        if (offsetProduct == null) {
            return offset;
        }

        Key key = Key.from(request);

        BigDecimal price = context.getMidPrice(key);

        if (price == null || price.signum() == 0) {
            return offset;
        }

        Key offsetKey = Key.build(key).site(offsetProduct.getKey()).instrument(offsetProduct.getValue()).build();

        BigDecimal offsetPrice = context.getMidPrice(offsetKey);

        if (offsetPrice == null || offsetPrice.signum() == 0) {
            return offset;
        }

        BigDecimal multiplier = configuration.getBigDecimal(KEY_MULTIPLIER, ZERO);

        BigDecimal basis = offsetPrice.divide(price, SCALE, HALF_UP).subtract(ONE)
                .min(PHYSICAL_RATE).max(PHYSICAL_RATE.negate()).multiply(multiplier);

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
