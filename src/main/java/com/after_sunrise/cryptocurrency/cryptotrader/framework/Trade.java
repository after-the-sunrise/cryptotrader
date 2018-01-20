package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface Trade {

    Instant getTimestamp();

    BigDecimal getPrice();

    BigDecimal getSize();

}
