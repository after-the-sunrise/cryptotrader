package com.after_sunrise.cryptocurrency.cryptotrader.service.bitpoint;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.google.gson.annotations.SerializedName;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BitpointExecution implements Order.Execution {

    public static final String SIDE_BUY = "3";

    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Tokyo"));

    @SerializedName("executionNo")
    private String id;

    @SerializedName("orderNo")
    private String orderId;

    @SerializedName("executionDt")
    private String date;

    @SerializedName("buySellCls")
    private String side;

    @SerializedName("execPrice")
    private BigDecimal price;

    @SerializedName("execAmount")
    private BigDecimal amount;

    @Override
    public Instant getTime() {
        return StringUtils.isNumeric(date) ? ZonedDateTime.parse(date, DATE_FORMAT).toInstant() : null;
    }

    @Override
    public BigDecimal getSize() {
        return amount == null ? null : SIDE_BUY.equals(side) ? amount : amount.negate();
    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Container {

        public static final Integer SUCCESS = 0;

        @SerializedName("resultCode")
        private Integer result;

        @SerializedName("executionList")
        private List<BitpointExecution> executions;

    }

}
