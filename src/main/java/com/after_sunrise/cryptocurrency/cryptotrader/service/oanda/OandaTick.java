package com.after_sunrise.cryptocurrency.cryptotrader.service.oanda;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

import static java.math.BigDecimal.ZERO;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OandaTick {

    @SerializedName("instrument")
    private String instrument;

    @SerializedName("time")
    private Instant timestamp;

    @SerializedName("status")
    private String status;

    @SerializedName("ask")
    private BigDecimal ask;

    @SerializedName("bid")
    private BigDecimal bid;

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class OandaTrade implements Trade {

        private static final BigDecimal HALF = new BigDecimal("0.5");

        private final OandaTick delegate;

        @Override
        public Instant getTimestamp() {
            return delegate.getTimestamp();
        }

        @Override
        public BigDecimal getPrice() {

            BigDecimal ask = delegate.getAsk();

            BigDecimal bid = delegate.getBid();

            return ask == null || bid == null ? null : ask.add(bid).multiply(HALF);

        }

        @Override
        public BigDecimal getSize() {
            return ZERO;
        }

        @Override
        public String getBuyOrderId() {
            return null;
        }

        @Override
        public String getSellOrderId() {
            return null;
        }

    }

}
