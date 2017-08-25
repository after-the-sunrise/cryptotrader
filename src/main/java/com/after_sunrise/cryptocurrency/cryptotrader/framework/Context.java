package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import lombok.*;

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

        public static Key from(Request request) {

            Key.KeyBuilder builder = Key.builder();

            if (request != null) {

                builder = builder.site(request.getSite());

                builder = builder.instrument(request.getInstrument());

                builder = builder.timestamp(request.getCurrentTime());

            }

            return builder.build();

        }

    }

    BigDecimal getBestAskPrice(Key key);

    BigDecimal getBestBidPrice(Key key);

    BigDecimal getMidPrice(Key key);

    BigDecimal getLastPrice(Key key);

    List<Trade> listTrades(Key key, Instant fromTime);

    BigDecimal getInstrumentPosition(Key key);

    BigDecimal getFundingPosition(Key key);

    BigDecimal roundLotSize(Key key, BigDecimal value, RoundingMode mode);

    BigDecimal roundTickSize(Key key, BigDecimal value, RoundingMode mode);

    BigDecimal getCommissionRate(Key key);

    Boolean isMarginable(Key key);

    Order findOrder(Key key, String id);

    List<Order> listActiveOrders(Key key);

    String createOrder(Key key, CreateInstruction instruction);

    String cancelOrder(Key key, CancelInstruction instruction);

}
