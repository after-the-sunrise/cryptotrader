package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Optional;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class TemplateAdviser implements Adviser {

    private static final int PRECISION = 12;

    private static final Advice BAIL = Advice.builder().build();

    private final String id;

    public TemplateAdviser(String id) {
        this.id = id;
    }

    @Override
    public String get() {
        return id;
    }

    @Override
    public Advice advise(Context context, Request request, Estimation estimation) {

        if (!Request.isValid(request)) {

            log.trace("Invalid request : {}", request);

            return BAIL;

        }

        if (estimation == null || estimation.getPrice() == null || estimation.getConfidence() == null) {

            log.trace("Invalid estimation : {}", estimation);

            return BAIL;

        }

        BigDecimal bPrice = calculateBuyLimitPrice(context, request, estimation);
        BigDecimal sPrice = calculateSellLimitPrice(context, request, estimation);
        BigDecimal bSize = calculateBuyLimitSize(context, request, bPrice);
        BigDecimal sSize = calculateSellLimitSize(context, request);

        return Advice.builder().buyLimitPrice(bPrice).buyLimitSize(bSize) //
                .sellLimitPrice(sPrice).sellLimitSize(sSize).build();

    }

    @VisibleForTesting
    BigDecimal calculateBuyLimitPrice(Context context, Request request, Estimation estimation) {

        Key key = Key.from(request);

        BigDecimal mid = context.getMidPrice(key);

        if (mid == null || mid.signum() <= 0) {

            log.trace("Buy price not available.");

            return null;

        }

        BigDecimal weighedEstimate = estimation.getPrice().multiply(estimation.getConfidence());

        BigDecimal base = mid.add(weighedEstimate).divide(ONE.add(estimation.getConfidence()), PRECISION, HALF_UP);

        BigDecimal spread = request.getTradingSpread();

        BigDecimal target = BigDecimal.ONE.subtract(spread).multiply(base);

        BigDecimal rounded = context.roundTickSize(key, target, DOWN);

        log.trace("Buy price : {} (base=[{}] spread=[{}] raw=[{}])", rounded, base, spread, target);

        return rounded;

    }

    @VisibleForTesting
    BigDecimal calculateSellLimitPrice(Context context, Request request, Estimation estimation) {

        Key key = Key.from(request);

        BigDecimal mid = context.getMidPrice(key);

        if (mid == null || mid.signum() <= 0) {

            log.trace("Sell price not available.");

            return null;

        }

        BigDecimal weighedEstimate = estimation.getPrice().multiply(estimation.getConfidence());

        BigDecimal base = mid.add(weighedEstimate).divide(ONE.add(estimation.getConfidence()), PRECISION, HALF_UP);

        BigDecimal spread = request.getTradingSpread();

        BigDecimal result = BigDecimal.ONE.add(spread).multiply(base);

        BigDecimal rounded = context.roundTickSize(key, result, UP);

        log.trace("Sell price : {} (base=[{}] spread=[{}] raw=[{}])", rounded, base, spread, result);

        return rounded;

    }

    @VisibleForTesting
    BigDecimal calculateBuyLimitSize(Context context, Request request, BigDecimal price) {

        if (price == null || price.signum() == 0) {

            log.trace("Invalid buy price : {}", price);

            return ZERO;

        }

        Key key = Key.from(request);

        BigDecimal fundAmount = context.getFundingPosition(key);

        if (fundAmount == null) {

            log.trace("Fund amount not available.");

            return ZERO;

        }

        BigDecimal productAmount = fundAmount.divide(price, PRECISION, DOWN);

        BigDecimal exposure = request.getTradingExposure();

        BigDecimal result = context.roundLotSize(key, productAmount.multiply(exposure), DOWN);

        log.trace("Buy size : {} (price=[{}] fund=[{}] exposure=[{}])", result, price, fundAmount, exposure);

        return Optional.ofNullable(result).orElse(ZERO);

    }

    @VisibleForTesting
    BigDecimal calculateSellLimitSize(Context context, Request request) {

        Key key = Key.from(request);

        BigDecimal position = context.getInstrumentPosition(key);

        if (position == null) {

            log.trace("Position not available.");

            return ZERO;

        }

        BigDecimal exposure = request.getTradingExposure();

        BigDecimal exposurePosition = position.multiply(exposure);

        BigDecimal result = context.roundLotSize(key, exposurePosition, DOWN);

        log.trace("Sell size : {} (position=[{}] exposure=[{}])", result, position, exposure);

        return Optional.ofNullable(result).orElse(ZERO);

    }

}
