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
public interface Trader extends Controllable {

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = PRIVATE)
    class Request {

        public static final String ALL = "*";

        private String site;

        private String instrument;

        private Instant timestamp;

        private BigDecimal tradingSpread;

        private BigDecimal tradingExposure;

        private BigDecimal tradingSplit;

        public boolean isValid() {

            if (StringUtils.isEmpty(site)) {
                return false;
            }

            if (StringUtils.isEmpty(instrument)) {
                return false;
            }

            if (timestamp == null) {
                return false;
            }

            if (tradingSpread == null) {
                return false;
            }

            if (tradingExposure == null) {
                return false;
            }

            if (tradingSplit == null) {
                return false;
            }

            return true;

        }

        public static boolean isValid(Request value) {
            return value != null && value.isValid();
        }

        public static boolean isInvalid(Request value) {
            return !isValid(value);
        }

    }

    void trade();

}
