package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.bitflyer4j.core.StateType;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.OrderList;
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

    }

    BigDecimal getBesAskPrice(Key key);

    BigDecimal getBesBidPrice(Key key);

    BigDecimal getMidPrice(Key key);

    BigDecimal getLastPrice(Key key);

    BigDecimal getInstrumentPosition(Key key);

    BigDecimal getFundingPosition(Key key);

    BigDecimal roundLotSize(Key key, BigDecimal value, RoundingMode mode);

    BigDecimal roundTickSize(Key key, BigDecimal value, RoundingMode mode);

    List<OrderList.Response> getOrders(Key key, StateType active);


}
