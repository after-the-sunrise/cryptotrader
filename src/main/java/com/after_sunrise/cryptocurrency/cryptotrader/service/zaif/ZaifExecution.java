package com.after_sunrise.cryptocurrency.cryptotrader.service.zaif;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ZaifExecution implements Order.Execution {

    private String id;

    private Data data;

    @Override
    public String getOrderId() {
        return id;
    }

    @Override
    public Instant getTime() {
        return data.getTimestamp();
    }

    @Override
    public BigDecimal getPrice() {
        return data.getPrice();
    }

    @Override
    public BigDecimal getSize() {
        return data.getQuantity();
    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Data {

        private static final Map<String, BigDecimal> SIDES = new HashMap<>();

        static {
            SIDES.put("bid", BigDecimal.ONE);
            SIDES.put("ask", BigDecimal.ONE.negate());
        }

        @SerializedName("currency_pair")
        private String code;

        @SerializedName("your_action")
        private String side;

        @SerializedName("amount")
        private BigDecimal size;

        @SerializedName("price")
        private BigDecimal price;

        @SerializedName("fee")
        private BigDecimal commission;

        @SerializedName("bonus")
        private BigDecimal refund;

        @SerializedName("timestamp")
        private Instant timestamp;

        @SerializedName("comment")
        private String comment;

        public BigDecimal getQuantity() {

            BigDecimal multiplier = SIDES.get(side);

            return multiplier == null || size == null ? null : multiplier.multiply(size);

        }

    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Container {

        private static final Integer SUCCESS = 1;

        @SerializedName("success")
        private Integer status;

        @SerializedName("return")
        private Map<String, Data> executions;

        public Boolean isSuccess() {
            return SUCCESS.equals(status) && executions != null;
        }

    }

}
