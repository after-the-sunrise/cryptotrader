package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.entity.Board.Quote;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Comparator;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface BitflyerService {

    String ID = "bitflyer";

    ZoneId ZONE = ZoneId.of("GMT");

    Comparator<BigDecimal> DECIMAL_COMPARATOR = Comparator //
            .nullsLast((o1, o2) -> Comparator.<BigDecimal>naturalOrder().compare(o1, o2));

    Comparator<Quote> QUOTE_COMPARATOR = Comparator //
            .nullsLast((o1, o2) -> DECIMAL_COMPARATOR.compare(o1.getPrice(), o2.getPrice()));

}
