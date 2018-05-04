package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateAdviser;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ProductType.BTC_JPY;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ProductType.FX_BTC_JPY;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.math.RoundingMode.UP;
import static java.util.Collections.unmodifiableNavigableMap;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitflyerAdviser extends TemplateAdviser implements BitflyerService {

    private static final BigDecimal SWAP_RATE = new BigDecimal("0.0004");

    private static final String KEY_SFD_PCT = "sfd.pct";

    private static final String KEY_SFD_INV = "sfd.inv";

    private static final String KEY_SFD_TBL = "sfd.tbl";

    private static final String KEY_SWAP_B = "swap.buy";

    private static final String KEY_SWAP_S = "swap.sell";

    private static final NavigableMap<BigDecimal, BigDecimal> SFD = unmodifiableNavigableMap(
            new TreeMap<>(Stream.of(
                    new SimpleEntry<>("0.1000", "0.0050"),
                    new SimpleEntry<>("0.1500", "0.0100"),
                    new SimpleEntry<>("0.2000", "0.0300")
            ).collect(Collectors.toMap(e -> new BigDecimal(e.getKey()), e -> new BigDecimal(e.getValue()))))
    );

    private final Cache<String, NavigableMap<BigDecimal, BigDecimal>> sfdCache;

    public BitflyerAdviser() {

        super(ID);

        sfdCache = CacheBuilder.newBuilder().maximumSize(10L).expireAfterAccess(10, MINUTES).build();

    }

    @VisibleForTesting
    BigDecimal calculateSwapRate(Context context, Request request, BigDecimal dailyRate) {

        if (dailyRate == null) {
            return ZERO;
        }

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

        double rate = Math.pow(ONE.add(dailyRate).doubleValue(), swaps) - 1;

        return BigDecimal.valueOf(rate).setScale(SCALE, UP);

    }

    @VisibleForTesting
    NavigableMap<BigDecimal, BigDecimal> getSfdTable() {

        String value = StringUtils.trimToEmpty(getStringProperty(KEY_SFD_TBL, null));

        NavigableMap<BigDecimal, BigDecimal> table = sfdCache.getIfPresent(value);

        if (table == null) {

            try {

                NavigableMap<BigDecimal, BigDecimal> m = new TreeMap<>();

                for (String entry : StringUtils.split(value, '|')) {

                    String[] kv = StringUtils.split(entry, ':');

                    BigDecimal k = new BigDecimal(kv[0]);

                    BigDecimal v = new BigDecimal(kv[1]);

                    m.put(k, v);

                }

                table = m.isEmpty() ? SFD : Collections.unmodifiableNavigableMap(m);

            } catch (Exception e) {

                table = SFD;

            }

            sfdCache.put(value, table);

        }

        return table;

    }

    @VisibleForTesting
    BigDecimal calculateSfdRate(Context context, Request request, boolean buy) {

        if (FX_BTC_JPY != ProductType.find(request.getInstrument())) {
            return ZERO;
        }

        BigDecimal sfdPct = getDecimalProperty(KEY_SFD_PCT, ONE);

        if (sfdPct.signum() == 0) {
            return ZERO;
        }

        Key key = Key.from(request);

        BigDecimal fxPrice = buy ? context.getBestAskPrice(key) : context.getBestBidPrice(key);

        if (fxPrice == null) {
            return ZERO;
        }

        BigDecimal cashPrice = context.getLastPrice(Key.build(key).instrument(BTC_JPY.name()).build());

        if (cashPrice == null || cashPrice.signum() == 0) {
            return ZERO;
        }

        BigDecimal pct = fxPrice.divide(cashPrice, SCALE, HALF_UP).subtract(ONE);

        BigDecimal adj = buy ? pct : pct.negate();

        NavigableMap<BigDecimal, BigDecimal> table = getSfdTable();

        Entry<BigDecimal, BigDecimal> floor = table.floorEntry(adj);

        if (floor == null) {
            return ZERO;
        }

        if (sfdPct.signum() > 0) {
            return floor.getValue().multiply(sfdPct);
        }

        Entry<BigDecimal, BigDecimal> higher = table.higherEntry(adj);

        if (higher == null) {
            return floor.getValue().multiply(sfdPct.negate());
        }

        BigDecimal numerator = adj.subtract(floor.getKey()).multiply(higher.getValue().subtract(floor.getValue()));

        BigDecimal denominator = higher.getKey().subtract(floor.getKey());

        BigDecimal interpolated = floor.getValue().add(numerator.divide(denominator, SCALE, HALF_UP));

        return interpolated.multiply(sfdPct.negate());

    }

    @VisibleForTesting
    BigDecimal calculateDualSfdRate(Context context, Request request, boolean buy) {

        BigDecimal rate = trimToZero(calculateSfdRate(context, request, buy));

        BigDecimal inversePct = getDecimalProperty(KEY_SFD_INV, ZERO);

        BigDecimal inverseRate = trimToZero(calculateSfdRate(context, request, !buy));

        return rate.max(inverseRate.multiply(inversePct));

    }

    @Override
    protected BigDecimal adjustBuyBasis(Context context, Request request, BigDecimal basis) {

        if (basis == null) {
            return null;
        }

        BigDecimal position = context.getInstrumentPosition(Key.from(request));

        BigDecimal swapRate = ZERO;

        if (trimToZero(position).signum() >= 0) {

            BigDecimal dailyRate = getDecimalProperty(KEY_SWAP_B, SWAP_RATE);

            swapRate = trimToZero(calculateSwapRate(context, request, dailyRate));

        }

        BigDecimal sfdRate = trimToZero(calculateDualSfdRate(context, request, true));

        return basis.add(swapRate).add(sfdRate);

    }

    @Override
    protected BigDecimal adjustSellBasis(Context context, Request request, BigDecimal basis) {

        if (basis == null) {
            return null;
        }

        BigDecimal position = context.getInstrumentPosition(Key.from(request));

        BigDecimal swapRate = ZERO;

        if (trimToZero(position).signum() <= 0) {

            BigDecimal dailyRate = getDecimalProperty(KEY_SWAP_S, SWAP_RATE);

            swapRate = trimToZero(calculateSwapRate(context, request, dailyRate));

        }

        BigDecimal sfdRate = trimToZero(calculateDualSfdRate(context, request, false));

        return basis.add(swapRate).add(sfdRate);

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

        BigDecimal dailyRate = getDecimalProperty(KEY_SWAP_B, SWAP_RATE);

        BigDecimal swap = calculateSwapRate(context, request, dailyRate);

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

        BigDecimal dailyRate = getDecimalProperty(KEY_SWAP_S, SWAP_RATE);

        BigDecimal swap = calculateSwapRate(context, request, dailyRate);

        if (ask == null || comm == null || swap == null) {
            return null;
        }

        BigDecimal theoretical = ask.multiply(ONE.add(comm).add(spread).add(swap));

        return price.max(theoretical);

    }

}
