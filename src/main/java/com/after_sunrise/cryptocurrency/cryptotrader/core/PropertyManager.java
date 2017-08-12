package com.after_sunrise.cryptocurrency.cryptotrader.core;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface PropertyManager {

    Instant getNow();

    String getVersion();

    Duration getTradingInterval();

    BigDecimal getTradingAggressiveness();

    Map<String, Set<String>> getTradingTargets();

}
