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
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.DOWN;
import static java.math.RoundingMode.UP;

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

        Advice.AdviceBuilder builder = Advice.builder();
        builder = builder.buyLimitPrice(calculateBuyLimitPrice(context, key));
        builder = builder.sellLimitPrice(calculateSellLimitPrice(context, key));
        builder = builder.buyLimitSize(calculateBuyLimitSize(context, key));
        builder = builder.sellLimitSize(calculateSellLimitSize(context, key));
        return builder.build();

    }

    @VisibleForTesting
    BigDecimal calculateBuyLimitPrice(Context context, Key key) {

        BigDecimal price = context.getBesAskPrice(key);

        if (price == null || price.signum() <= 0) {

            log.trace("Ask price not available.");

            return null;

        }

        BigDecimal spread = propertyManager.getTradingSpread();

        BigDecimal result = BigDecimal.ONE.subtract(spread).multiply(price);

        BigDecimal rounded = context.roundFundingPosition(key, result, DOWN);

        log.trace("Buy price : {} (ask=[{}] spread=[{}] raw=[{}])", rounded, price, spread, result);

        return rounded;

    }

    @VisibleForTesting
    BigDecimal calculateSellLimitPrice(Context context, Key key) {

        BigDecimal price = context.getBesBidPrice(key);

        if (price == null) {

            log.trace("Bid price not available.");

            return null;

        }

        BigDecimal spread = propertyManager.getTradingSpread();

        BigDecimal result = BigDecimal.ONE.add(spread).multiply(price);

        BigDecimal rounded = context.roundFundingPosition(key, result, UP);

        log.trace("Sell price : {} (ask=[{}] spread=[{}] raw=[{}])", rounded, price, spread, result);

        return rounded;

    }

    @VisibleForTesting
    BigDecimal calculateBuyLimitSize(Context context, Key key) {

        BigDecimal productPrice = calculateBuyLimitPrice(context, key);

        if (productPrice == null || productPrice.signum() == 0) {

            log.trace("Invalid buy price : {}", productPrice);

            return ZERO;

        }

        BigDecimal fundAmount = context.getFundingPosition(key);

        if (fundAmount == null) {

            log.trace("Fund amount not available.");

            return ZERO;

        }

        BigDecimal productAmount = fundAmount.divide(productPrice, PRECISION, DOWN);

        BigDecimal exposure = propertyManager.getTradingExposure();

        BigDecimal result = context.roundInstrumentPosition(key, productAmount.multiply(exposure), DOWN);

        log.trace("Buy size : {} (price=[{}] fund=[{}] exposure=[{}])", result, productPrice, fundAmount, exposure);

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
