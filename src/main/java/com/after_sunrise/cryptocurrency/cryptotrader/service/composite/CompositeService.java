package com.after_sunrise.cryptocurrency.cryptotrader.service.composite;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.AbstractEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.LastEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.MidEstimator;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.ArrayUtils;

import java.math.BigDecimal;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.HALF_UP;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface CompositeService extends Service {

    class CompositeEstimator extends AbstractEstimator implements Estimator {

        private static final String DELIMITER = "|";

        private static final String INSTRUMENT = ":";

        private static final Map<Character, BinaryOperator<BigDecimal>> OPERATORS = Stream.of(
                new SimpleEntry<>('+', (BinaryOperator<BigDecimal>) BigDecimal::add),
                new SimpleEntry<>('-', (BinaryOperator<BigDecimal>) BigDecimal::subtract),
                new SimpleEntry<>('*', (BinaryOperator<BigDecimal>) BigDecimal::multiply),
                new SimpleEntry<>('/', (BinaryOperator<BigDecimal>) (o1, o2) -> o1.divide(o2, SCALE, HALF_UP))
        ).collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        @Override
        public Estimation estimate(Context context, Request request) {
            return BAIL;
        }

        @VisibleForTesting
        Estimation estimateComposite(Context context, Key key, BiFunction<Context, Key, Estimation> function) {

            Key.KeyBuilder builder = Key.build(key);

            String[] elements = split(trimToEmpty(key.getInstrument()), DELIMITER);

            if (ArrayUtils.isEmpty(elements)) {
                return BAIL;
            }

            BigDecimal price = ONE;

            BigDecimal confidence = ONE;

            for (String target : elements) {

                String[] kv = split(target, INSTRUMENT, 2);

                if (kv.length != 2) {
                    return BAIL;
                }

                if (kv[0].length() <= 1) {
                    return BAIL;
                }

                BinaryOperator<BigDecimal> operator = OPERATORS.get(kv[0].charAt(0));

                if (operator == null) {
                    return BAIL;
                }

                Key elementKey = builder.site(kv[0].substring(1)).instrument(kv[1]).build();

                Estimation estimation = function.apply(context, elementKey);

                if (estimation == null || estimation.getPrice() == null || estimation.getConfidence() == null) {
                    return BAIL;
                }

                price = operator.apply(price, estimation.getPrice());

                confidence = confidence.multiply(estimation.getConfidence());

            }

            return Estimation.builder().price(price).confidence(confidence).build();

        }

    }

    class CompositeMidEstimator extends MidEstimator {

        private static final CompositeEstimator DELEGATE = new CompositeEstimator();

        @Override
        public Estimation estimate(Context context, Key key) {
            return DELEGATE.estimateComposite(context, key, (c, k) -> super.estimate(c, k));
        }

    }

    class CompositeLastEstimator extends LastEstimator {

        private static final CompositeEstimator DELEGATE = new CompositeEstimator();

        @Override
        public Estimation estimate(Context context, Key key) {
            return DELEGATE.estimateComposite(context, key, (c, k) -> super.estimate(c, k));
        }

    }

}
