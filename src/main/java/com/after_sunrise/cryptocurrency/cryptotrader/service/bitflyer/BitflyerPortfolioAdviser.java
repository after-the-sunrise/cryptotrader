package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.cryptotrader.core.PropertyManager;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.MarketEstimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.PortfolioAdviser;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.impl.Frameworks;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.impl.Frameworks.trimToZero;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class BitflyerPortfolioAdviser implements PortfolioAdviser {

    private static final int PRECISION = 12;

    private static final Advice BAIL = Advice.builder().build();

    private PropertyManager propertyManager;

    @Inject
    public void initialize(Injector injector) {

        this.propertyManager = injector.getInstance(PropertyManager.class);

        log.debug("Initialized.");

    }

    @Override
    public String get() {
        return BitflyerService.ID;
    }

    @Override
    public Advice advise(Context context, Request request, Estimation estimation) {

        if (Frameworks.isInvalid(request)) {

            log.trace("Invalid request : {}", request);

            return BAIL;

        }

        if (estimation == null || estimation.getPrice() == null || estimation.getConfidence() == null) {

            log.trace("Invalid estimation : {}", estimation);

            return BAIL;

        }

        Key key = Frameworks.convert(request);

        BigDecimal bPrice = calculateBuyLimitPrice(context, key, estimation);
        BigDecimal sPrice = calculateSellLimitPrice(context, key, estimation);
        BigDecimal bSize = calculateBuyLimitSize(context, key, bPrice);
        BigDecimal sSize = calculateSellLimitSize(context, key);

        return Advice.builder().buyLimitPrice(bPrice).buyLimitSize(bSize) //
                .sellLimitPrice(sPrice).sellLimitSize(sSize).build();

    }

    @VisibleForTesting
    BigDecimal calculateBuyLimitPrice(Context context, Key key, Estimation estimation) {

        BigDecimal mid = context.getMidPrice(key);

        if (mid == null || mid.signum() <= 0) {

            log.trace("Buy price not available.");

            return null;

        }

        BigDecimal weighedEstimate = estimation.getPrice().multiply(estimation.getConfidence());

        BigDecimal base = mid.add(weighedEstimate).divide(ONE.add(estimation.getConfidence()), PRECISION, HALF_UP);

        BigDecimal spread = propertyManager.getTradingSpread();

        BigDecimal target = BigDecimal.ONE.subtract(spread).multiply(base);

        BigDecimal rounded = context.roundFundingPosition(key, target, DOWN);

        log.trace("Buy price : {} (base=[{}] spread=[{}] raw=[{}])", rounded, base, spread, target);

        return rounded;

    }

    @VisibleForTesting
    BigDecimal calculateSellLimitPrice(Context context, Key key, Estimation estimation) {

        BigDecimal mid = context.getMidPrice(key);

        if (mid == null || mid.signum() <= 0) {

            log.trace("Sell price not available.");

            return null;

        }

        BigDecimal weighedEstimate = estimation.getPrice().multiply(estimation.getConfidence());

        BigDecimal base = mid.add(weighedEstimate).divide(ONE.add(estimation.getConfidence()), PRECISION, HALF_UP);

        BigDecimal spread = propertyManager.getTradingSpread();

        BigDecimal result = BigDecimal.ONE.add(spread).multiply(base);

        BigDecimal rounded = context.roundFundingPosition(key, result, UP);

        log.trace("Sell price : {} (base=[{}] spread=[{}] raw=[{}])", rounded, base, spread, result);

        return rounded;

    }

    @VisibleForTesting
    BigDecimal calculateBuyLimitSize(Context context, Key key, BigDecimal price) {

        if (price == null || price.signum() == 0) {

            log.trace("Invalid buy price : {}", price);

            return ZERO;

        }

        BigDecimal fundAmount = context.getFundingPosition(key);

        if (fundAmount == null) {

            log.trace("Fund amount not available.");

            return ZERO;

        }

        BigDecimal productAmount = fundAmount.divide(price, PRECISION, DOWN);

        BigDecimal exposure = propertyManager.getTradingExposure();

        BigDecimal result = context.roundInstrumentPosition(key, productAmount.multiply(exposure), DOWN);

        log.trace("Buy size : {} (price=[{}] fund=[{}] exposure=[{}])", result, price, fundAmount, exposure);

        return trimToZero(result);

    }

    @VisibleForTesting
    BigDecimal calculateSellLimitSize(Context context, Key key) {

        BigDecimal position = context.getInstrumentPosition(key);

        if (position == null) {

            log.trace("Position not available.");

            return ZERO;

        }

        BigDecimal exposure = propertyManager.getTradingExposure();

        BigDecimal exposurePosition = position.multiply(exposure);

        BigDecimal result = context.roundInstrumentPosition(key, exposurePosition, DOWN);

        log.trace("Sell size : {} (position=[{}] exposure=[{}])", result, position, exposure);

        return trimToZero(result);

    }

}
