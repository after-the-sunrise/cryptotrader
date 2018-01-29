package com.after_sunrise.cryptocurrency.cryptotrader.service.btcbox;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.btcbox.BtcboxService.ProductType.BTC_JPY;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BtcboxOrder implements Order {

    @SerializedName("id")
    private String id;

    @SerializedName("datetime")
    private ZonedDateTime timestamp;

    @SerializedName("type")
    private String side;

    @SerializedName("status")
    private String status;

    @SerializedName("price")
    private BigDecimal orderPrice;

    @SerializedName("amount_original")
    private BigDecimal orderQuantity;

    @SerializedName("amount_outstanding")
    private BigDecimal remainingQuantity;

    @Override
    public String getProduct() {
        return BTC_JPY.name();
    }

    @Override
    public Boolean getActive() {
        // no, part, cancelled, all
        return !"cancelled".equals(status) && !"all".equals(status);
    }

    @VisibleForTesting
    BigDecimal convertQuantity(BigDecimal value) {

        if (value != null) {

            if ("buy".equals(side)) {
                return value;
            }

            if ("sell".equals(side)) {
                return value.negate();
            }

        }

        return null;

    }

    @Override
    public BigDecimal getOrderQuantity() {
        return convertQuantity(orderQuantity);
    }

    @Override
    public BigDecimal getFilledQuantity() {

        BigDecimal total = getOrderQuantity();

        if (total == null) {
            return null;
        }

        BigDecimal pending = getRemainingQuantity();

        if (pending == null) {
            return null;
        }

        return convertQuantity(total.subtract(pending));

    }

    @Override
    public BigDecimal getRemainingQuantity() {
        return convertQuantity(remainingQuantity);
    }

    public class BtcboxExecution implements Execution {

        @Override
        public String getId() {
            return BtcboxOrder.this.id;
        }

        @Override
        public String getOrderId() {
            return BtcboxOrder.this.id;
        }

        @Override
        public Instant getTime() {
            return BtcboxOrder.this.timestamp == null ? null : BtcboxOrder.this.timestamp.toInstant();
        }

        @Override
        public BigDecimal getPrice() {
            return BtcboxOrder.this.orderPrice;
        }

        @Override
        public BigDecimal getSize() {

            BigDecimal filled = BtcboxOrder.this.getFilledQuantity();

            return filled == null ? null : filled.abs();

        }

    }

}
