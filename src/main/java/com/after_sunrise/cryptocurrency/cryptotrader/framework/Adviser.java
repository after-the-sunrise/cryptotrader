package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface Adviser extends Service {

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor(access = PRIVATE)
    class Advice {

        private final BigDecimal buyLimitPrice;

        private final BigDecimal buyLimitSize;

        private final BigDecimal buySpread;

        private final BigDecimal sellLimitPrice;

        private final BigDecimal sellLimitSize;

        private final BigDecimal sellSpread;

    }

    Advice advise(Context context, Request request, Estimation estimation);

}
