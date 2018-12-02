package com.after_sunrise.cryptocurrency.cryptotrader.service.bitpoint;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BitpointDepth {

    private static final Comparator<BigDecimal> NATURAL = Comparator.naturalOrder();

    private static final Comparator<BigDecimal> REVERSE = Comparator.reverseOrder();

    @SerializedName("asks")
    private List<Quote> asks;

    @SerializedName("bids")
    private List<Quote> bids;

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Quote {

        @SerializedName("price")
        private BigDecimal price;

        @SerializedName("qty")
        private BigDecimal quantity;

        @SerializedName("ignore")
        private Boolean ignore;

    }

    @VisibleForTesting
    NavigableMap<BigDecimal, BigDecimal> convert(List<Quote> values, Comparator<BigDecimal> comparator) {

        if (values == null) {
            return null;
        }

        NavigableMap<BigDecimal, BigDecimal> map = new TreeMap<>(comparator);

        values.stream()
                .filter(Objects::nonNull)
                .filter(q -> q.getPrice() != null)
                .filter(q -> q.getPrice().signum() != 0)
                .filter(q -> q.getQuantity() != null)
                .filter(q -> q.getQuantity().signum() != 0)
                .filter(q -> q.getIgnore() != Boolean.TRUE)
                .forEach(q -> map.put(q.getPrice(), q.getQuantity()));
        ;

        return Collections.unmodifiableNavigableMap(map);

    }

    public NavigableMap<BigDecimal, BigDecimal> getAskPrices() {
        return convert(asks, NATURAL);
    }

    public NavigableMap<BigDecimal, BigDecimal> getBidPrices() {
        return convert(bids, REVERSE);
    }

}
