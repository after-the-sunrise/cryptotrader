package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.MarketEstimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.function.Supplier;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface PortfolioAdviser extends Supplier<String> {

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = PRIVATE)
    class Advice {

        private final BigDecimal buyLimitPrice;

        private final BigDecimal buyLimitSize;

        private final BigDecimal sellLimitPrice;

        private final BigDecimal sellLimitSize;

    }

    Advice advise(Context context, Request request, Estimation estimation);

}
