package com.after_sunrise.cryptocurrency.cryptotrader.service.zaif;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ZERO;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ZaifOrder implements Order {

    private String id;

    private Data data;

    @Override
    public String getProduct() {
        return data.getCode();
    }

    @Override
    public Boolean getActive() {
        return TRUE;
    }

    @Override
    public BigDecimal getOrderPrice() {
        return data.getPrice();
    }

    @Override
    public BigDecimal getOrderQuantity() {
        return data.getQuantity();
    }

    @Override
    public BigDecimal getFilledQuantity() {
        return ZERO;
    }

    @Override
    public BigDecimal getRemainingQuantity() {
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

        @SerializedName("action")
        private String side;

        @SerializedName("amount")
        private BigDecimal size;

        @SerializedName("price")
        private BigDecimal price;

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
        private Map<String, Data> orders;

        public Boolean isSuccess() {
            return SUCCESS.equals(status) && orders != null;
        }

        public Map<String, Data> getOrders() {
            return orders;
        }

    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Response {

        private static final Integer SUCCESS = 1;

        @SerializedName("success")
        private Integer status;

        @SerializedName("return")
        private Map<String, String> data;

        public Boolean isSuccess() {
            return SUCCESS.equals(status) && data != null;
        }

        public String getOrderId() {
            return data == null ? null : data.get("order_id");
        }

    }

}
