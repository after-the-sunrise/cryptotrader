package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface Context extends Supplier<String> {

    @Getter
    @Builder
    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor(access = PRIVATE)
    class Key {

        private final String site;

        private final String instrument;

        private final Instant timestamp;

        public static boolean isValid(Key value) {

            if (value == null) {
                return false;
            }

            if (StringUtils.isEmpty(value.getSite())) {
                return false;
            }

            if (StringUtils.isEmpty(value.getInstrument())) {
                return false;
            }

            if (value.getTimestamp() == null) {
                return false;
            }

            return true;

        }

        public static Key from(Trader.Request request) {

            Key.KeyBuilder builder = Key.builder();

            if (request != null) {

                builder = builder.site(request.getSite());

                builder = builder.instrument(request.getInstrument());

                builder = builder.timestamp(request.getTimestamp());

            }

            return builder.build();

        }

    }

    BigDecimal getBestAskPrice(Key key);

    BigDecimal getBestBidPrice(Key key);

    BigDecimal getMidPrice(Key key);

    BigDecimal getLastPrice(Key key);

    BigDecimal getInstrumentPosition(Key key);

    BigDecimal getFundingPosition(Key key);

    BigDecimal roundLotSize(Key key, BigDecimal value, RoundingMode mode);

    BigDecimal roundTickSize(Key key, BigDecimal value, RoundingMode mode);

    Order findOrder(Key key, String id);

    List<Order> listOrders(Key key);

    String createOrder(Key key, CreateInstruction instruction);

    String cancelOrder(Key key, CancelInstruction instruction);

}
