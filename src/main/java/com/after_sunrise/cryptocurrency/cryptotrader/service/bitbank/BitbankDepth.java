package com.after_sunrise.cryptocurrency.cryptotrader.service.bitbank;

import cc.bitbank.entity.Depth;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.ArrayUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@ToString
public class BitbankDepth {

    private static final Comparator<BigDecimal> NATURAL = Comparator.nullsLast(Comparator.naturalOrder());

    private static final Comparator<BigDecimal> REVERSE = Comparator.nullsLast(Comparator.reverseOrder());

    private final Instant timestamp;

    private final NavigableMap<BigDecimal, BigDecimal> asks;

    private final NavigableMap<BigDecimal, BigDecimal> bids;

    public BitbankDepth(Depth depth) {

        timestamp = depth.timestamp == null ? null : Instant.ofEpochMilli(depth.timestamp.getTime());

        asks = convert(depth.asks, NATURAL);

        bids = convert(depth.bids, REVERSE);

    }

    @VisibleForTesting
    NavigableMap<BigDecimal, BigDecimal> convert(BigDecimal[][] quotes, Comparator<BigDecimal> c) {

        NavigableMap<BigDecimal, BigDecimal> map = new TreeMap<>(c);

        if (ArrayUtils.isNotEmpty(quotes)) {

            for (BigDecimal[] quote : quotes) {

                if (quote.length != 2) {
                    continue;
                }

                if (quote[0] == null || quote[0].signum() == 0) {
                    continue;
                }

                if (quote[1] == null || quote[1].signum() == 0) {
                    continue;
                }

                map.put(quote[0], quote[1]);

            }

        }

        return Collections.unmodifiableNavigableMap(map);

    }

}
