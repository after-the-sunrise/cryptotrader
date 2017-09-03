package com.after_sunrise.cryptocurrency.cryptotrader.core;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface PropertyManager extends Environment {

    Map<String, Set<String>> getTradingTargets();

    Duration getTradingInterval();

    Integer getTradingThreads();

    Boolean getTradingActive(String site, String instrument);

    BigDecimal getTradingSpread(String site, String instrument);

    BigDecimal getTradingExposure(String site, String instrument);

    BigDecimal getTradingSplit(String site, String instrument);

    Duration getTradingDuration(String site, String instrument);

    BigDecimal getFundingOffset(String site, String instrument);

    Set<String> getEstimators(String site, String instrument);

}
