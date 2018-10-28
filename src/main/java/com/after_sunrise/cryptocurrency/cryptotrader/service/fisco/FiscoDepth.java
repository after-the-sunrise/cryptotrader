package com.after_sunrise.cryptocurrency.cryptotrader.service.fisco;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.annotations.SerializedName;
import lombok.*;
import org.apache.commons.lang3.ArrayUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FiscoDepth {

    private static final Comparator<BigDecimal> NATURAL = Comparator.naturalOrder();

    private static final Comparator<BigDecimal> REVERSE = Comparator.reverseOrder();

    @SerializedName("asks")
    private BigDecimal[][] asks;

    @SerializedName("bids")
    private BigDecimal[][] bids;

    @VisibleForTesting
    NavigableMap<BigDecimal, BigDecimal> convert(BigDecimal[][] values, Comparator<BigDecimal> comparator) {

        if (values == null) {
            return null;
        }

        NavigableMap<BigDecimal, BigDecimal> map = new TreeMap<>(comparator);

        Stream.of(values)
                .filter(ArrayUtils::isNotEmpty)
                .filter(ps -> ps.length == 2)
                .filter(ps -> ps[0] != null)
                .filter(ps -> ps[1] != null)
                .forEach(ps -> map.put(ps[0], ps[1]))
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
