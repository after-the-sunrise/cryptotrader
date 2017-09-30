package com.after_sunrise.cryptocurrency.cryptotrader.service.composite;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MidEstimator;
import com.google.common.annotations.VisibleForTesting;

import java.math.BigDecimal;

import static java.math.BigDecimal.ONE;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface CompositeService extends Service {

    class CompositeMidEstimator extends MidEstimator {

        private static final String DELIMITER = "|";

        private static final String INSTRUMENT = ":";

        @VisibleForTesting
        protected Key getKey(Request request) {
            return super.getKey(request);
        }

        @VisibleForTesting
        protected Estimation estimate(Context context, Key key) {
            return super.estimate(context, key);
        }

        @Override
        public Estimation estimate(Context context, Request request) {

            Key compositeKey = getKey(request);

            Key.KeyBuilder builder = Key.build(compositeKey);

            BigDecimal price = ONE;

            BigDecimal confidence = ONE;

            for (String target : split(trimToEmpty(compositeKey.getInstrument()), DELIMITER)) {

                String[] kv = split(target, INSTRUMENT, 2);

                if (kv.length != 2) {
                    continue;
                }

                Key key = builder.site(kv[0]).instrument(kv[1]).build();

                Estimation estimation = estimate(context, key);

                if (estimation == null || estimation.getPrice() == null || estimation.getConfidence() == null) {
                    return BAIL;
                }

                price = price.multiply(estimation.getPrice());

                confidence = confidence.multiply(estimation.getConfidence());

            }

            return Estimation.builder().price(price).confidence(confidence).build();

        }

    }

}
