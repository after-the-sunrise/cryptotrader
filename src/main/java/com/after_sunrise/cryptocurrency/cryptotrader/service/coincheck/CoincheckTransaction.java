package com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.google.gson.annotations.SerializedName;
import lombok.*;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CoincheckTransaction implements Order.Execution {

    @SerializedName("id")
    private String id;

    @SerializedName("order_id")
    private String orderId;

    @SerializedName("created_at")
    private Instant time;

    @SerializedName("pair")
    private String product;

    @SerializedName("rate")
    private BigDecimal price;

    @SerializedName("funds")
    private Map<String, BigDecimal> funds;

    @Override
    public BigDecimal getSize() {

        if (StringUtils.isEmpty(product) || MapUtils.isEmpty(funds)) {
            return null;
        }

        String code = StringUtils.split(product, '_')[0];

        return funds.get(code);

    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Container {

        @SerializedName("success")
        private Boolean success;

        @SerializedName("transactions")
        private List<CoincheckTransaction> transactions;

    }

}
