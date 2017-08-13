package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.entity.Board.Quote;
import lombok.Getter;

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

    enum ProductType {

        JPY("JPY", "JPY", new BigDecimal("1")),

        BTC("BTC", "BTC", new BigDecimal("0.00000001")),

        BTH("BTH", "BTH", new BigDecimal("0.00000001")),

        ETH("ETH", "ETH", new BigDecimal("0.00000001")),

        ETC("ETC", "ETC", new BigDecimal("0.00000001")),

        LTC("LTC", "LTC", new BigDecimal("0.00000001")),

        BTC_JPY("BTC", "JPY", new BigDecimal("0.00000001")),

        BCH_JPY("BCH", "JPY", new BigDecimal("0.001")),

        ETH_BTC("ETH", "BTC", new BigDecimal("0.001"));

        @Getter
        private final String structureCode;

        @Getter
        private final String fundCode;

        @Getter
        private final BigDecimal unit;

        ProductType(String structureCode, String fundCode, BigDecimal unit) {
            this.structureCode = structureCode;
            this.fundCode = fundCode;
            this.unit = unit;
        }

        public static ProductType find(String name) {

            for (ProductType type : values()) {
                if (type.name().equals(name)) {
                    return type;
                }
            }

            return null;

        }

    }

}
