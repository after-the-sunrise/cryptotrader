package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface Trader {

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = PRIVATE)
    class Request {

        public static final String ALL = "*";

        private String site;

        private String instrument;

        private Instant timestamp;

        private BigDecimal aggressiveness;

        public static boolean isValid(Request value) {

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

            if (value.getAggressiveness() == null) {
                return false;
            }

            return true;

        }

    }

    void trade();

}
