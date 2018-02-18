package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateAdviser;
import com.google.common.annotations.VisibleForTesting;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap.SimpleEntry;
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

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitflyerAdviser extends TemplateAdviser implements BitflyerService {

    private static final BigDecimal SWAP_RATE = new BigDecimal("0.0004");

    private static final String KEY_SFD_PAD = "sfd.pad";

    private static final String KEY_SFD_PCT = "sfd.pct";

    private static final String KEY_SWAP_B = "swap.buy";

    private static final String KEY_SWAP_S = "swap.sell";

    private static final NavigableMap<BigDecimal, BigDecimal> SFD = unmodifiableNavigableMap(
            new TreeMap<>(Stream.of(
                    new SimpleEntry<>("0.0000", "0.0000"),
                    // Tier 1 (10% ~ 15% -> 50 bps)
                    new SimpleEntry<>("0.0955", "0.0005"),
                    new SimpleEntry<>("0.0960", "0.0010"),
                    new SimpleEntry<>("0.0965", "0.0015"),
                    new SimpleEntry<>("0.0970", "0.0020"),
                    new SimpleEntry<>("0.0975", "0.0025"),
                    new SimpleEntry<>("0.0980", "0.0030"),
                    new SimpleEntry<>("0.0985", "0.0035"),
                    new SimpleEntry<>("0.0990", "0.0040"),
                    new SimpleEntry<>("0.0995", "0.0045"),
                    new SimpleEntry<>("0.1000", "0.0050"),
                    // Tier 2 (15% ~ 20% -> 100 bps)
                    new SimpleEntry<>("0.1455", "0.0055"),
                    new SimpleEntry<>("0.1460", "0.0060"),
                    new SimpleEntry<>("0.1465", "0.0065"),
                    new SimpleEntry<>("0.1470", "0.0070"),
                    new SimpleEntry<>("0.1475", "0.0075"),
                    new SimpleEntry<>("0.1480", "0.0080"),
                    new SimpleEntry<>("0.1485", "0.0085"),
                    new SimpleEntry<>("0.1490", "0.0090"),
                    new SimpleEntry<>("0.1495", "0.0095"),
                    new SimpleEntry<>("0.1500", "0.0100"),
                    // Tier 3 (20% ~ **% -> 300 bps)
                    new SimpleEntry<>("0.1905", "0.0110"),
                    new SimpleEntry<>("0.1910", "0.0120"),
                    new SimpleEntry<>("0.1915", "0.0130"),
                    new SimpleEntry<>("0.1920", "0.0140"),
                    new SimpleEntry<>("0.1925", "0.0150"),
                    new SimpleEntry<>("0.1930", "0.0160"),
                    new SimpleEntry<>("0.1935", "0.0170"),
                    new SimpleEntry<>("0.1940", "0.0180"),
                    new SimpleEntry<>("0.1945", "0.0190"),
                    new SimpleEntry<>("0.1950", "0.0200"),
                    new SimpleEntry<>("0.1955", "0.0210"),
                    new SimpleEntry<>("0.1960", "0.0220"),
                    new SimpleEntry<>("0.1965", "0.0230"),
                    new SimpleEntry<>("0.1970", "0.0240"),
                    new SimpleEntry<>("0.1975", "0.0250"),
                    new SimpleEntry<>("0.1980", "0.0260"),
                    new SimpleEntry<>("0.1985", "0.0270"),
                    new SimpleEntry<>("0.1990", "0.0280"),
                    new SimpleEntry<>("0.1995", "0.0290"),
                    new SimpleEntry<>("0.2000", "0.0300")
            ).collect(Collectors.toMap(e -> new BigDecimal(e.getKey()), e -> new BigDecimal(e.getValue()))))
    );

    public BitflyerAdviser() {
        super(ID);
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
    BigDecimal calculateSfdRate(Context context, Request request, boolean buy) {

        if (FX_BTC_JPY != ProductType.find(request.getInstrument())) {
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

        BigDecimal pad = getDecimalProperty(KEY_SFD_PAD, ZERO);

        BigDecimal adj = buy ? pct.add(pad) : pct.negate().add(pad);

        Entry<BigDecimal, BigDecimal> tier = SFD.floorEntry(adj.max(SFD.firstKey()));

        BigDecimal sfdPct = getDecimalProperty(KEY_SFD_PCT, ONE);

        return tier.getValue().multiply(sfdPct);

    }

    @Override
    protected BigDecimal adjustBuyBasis(Context context, Request request, BigDecimal basis) {

        if (basis == null) {
            return null;
        }

        BigDecimal dailyRate = getDecimalProperty(KEY_SWAP_B, SWAP_RATE);

        BigDecimal swapRate = trimToZero(calculateSwapRate(context, request, dailyRate));

        BigDecimal sfdRate = trimToZero(calculateSfdRate(context, request, true));

        return basis.add(swapRate).add(sfdRate);

    }

    @Override
    protected BigDecimal adjustSellBasis(Context context, Request request, BigDecimal basis) {

        if (basis == null) {
            return null;
        }

        BigDecimal dailyRate = getDecimalProperty(KEY_SWAP_S, SWAP_RATE);

        BigDecimal swapRate = trimToZero(calculateSwapRate(context, request, dailyRate));

        BigDecimal sfdRate = trimToZero(calculateSfdRate(context, request, false));

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
