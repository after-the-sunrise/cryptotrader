package com.after_sunrise.cryptocurrency.cryptotrader.core;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface PropertyController extends PropertyManager {

    void setTradingTargets(Map<String, Set<String>> values);

    void setTradingInterval(Duration value);

    void setTradingActive(String site, String instrument, Boolean value);

    void setTradingSpread(String site, String instrument, BigDecimal value);

    void setTradingExposure(String site, String instrument, BigDecimal value);

    void setTradingSplit(String site, String instrument, BigDecimal value);

    void setTradingDuration(String site, String instrument, Duration value);

    void setFundingOffset(String site, String instrument, BigDecimal value);

    void setEstimators(String site, String instrument, Set<String> values);

}
