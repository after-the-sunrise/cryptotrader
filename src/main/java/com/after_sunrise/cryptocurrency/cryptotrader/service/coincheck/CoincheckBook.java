package com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck;

import com.google.gson.annotations.SerializedName;
import lombok.*;
import org.apache.commons.collections4.CollectionUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.reverseOrder;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CoincheckBook {

    private static final int I_PRICE = 0;

    private static final int I_SIZE = 1;

    @SerializedName("asks")
    private List<BigDecimal[]> asks;

    @SerializedName("bids")
    private List<BigDecimal[]> bids;

    private BigDecimal extractBest(List<BigDecimal[]> input, boolean ascending, int index) {

        if (CollectionUtils.isEmpty(input)) {
            return null;
        }

        Comparator<BigDecimal> comparator = ascending ? naturalOrder() : reverseOrder();

        return input.stream()
                .filter(Objects::nonNull)
                .filter(ps -> ps.length == 2)
                .filter(ps -> ps[I_PRICE] != null)
                .filter(ps -> ps[I_SIZE] != null)
                .sorted((ps1, ps2) -> comparator.compare(ps1[I_PRICE], ps2[I_PRICE]))
                .map(ps -> ps[index])
                .findFirst().orElse(null);

    }

    public BigDecimal getBestAskPrice() {
        return extractBest(asks, true, I_PRICE);
    }

    public BigDecimal getBestBidPrice() {
        return extractBest(bids, false, I_PRICE);
    }

    public BigDecimal getBestAskSize() {
        return extractBest(asks, true, I_SIZE);
    }

    public BigDecimal getBestBidSize() {
        return extractBest(bids, false, I_SIZE);
    }

}
