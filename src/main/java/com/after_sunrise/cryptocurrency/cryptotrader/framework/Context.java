package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface Context extends Service, AutoCloseable {

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

        public static Key.KeyBuilder build(Key key) {

            Key.KeyBuilder builder = Key.builder();

            if (key != null) {

                builder = builder.site(key.getSite());

                builder = builder.instrument(key.getInstrument());

                builder = builder.timestamp(key.getTimestamp());

            }

            return builder;

        }

    }

    enum StateType {
        ACTIVE, TERMINATE;
    }

    StateType getState(Key key);

    BigDecimal getBestAskPrice(Key key);

    BigDecimal getBestBidPrice(Key key);

    BigDecimal getBestAskSize(Key key);

    BigDecimal getBestBidSize(Key key);

    BigDecimal getMidPrice(Key key);

    BigDecimal getLastPrice(Key key);

    List<Trade> listTrades(Key key, Instant fromTime);

    CurrencyType getInstrumentCurrency(Key key);

    CurrencyType getFundingCurrency(Key key);

    BigDecimal getConversionPrice(Key key, CurrencyType currency);

    BigDecimal getInstrumentPosition(Key key);

    BigDecimal getFundingPosition(Key key);

    BigDecimal roundLotSize(Key key, BigDecimal value, RoundingMode mode);

    BigDecimal roundTickSize(Key key, BigDecimal value, RoundingMode mode);

    BigDecimal getCommissionRate(Key key);

    Boolean isMarginable(Key key);

    ZonedDateTime getExpiry(Key key);

    Order findOrder(Key key, String id);

    List<Order> listActiveOrders(Key key);

    List<Order.Execution> listExecutions(Key key);

    Map<CreateInstruction, String> createOrders(Key key, Set<CreateInstruction> instructions);

    Map<CancelInstruction, String> cancelOrders(Key key, Set<CancelInstruction> instructions);

}
