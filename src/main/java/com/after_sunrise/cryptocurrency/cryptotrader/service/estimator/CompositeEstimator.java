package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.HALF_UP;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class CompositeEstimator extends AbstractEstimator {

    private static final Map<Character, BinaryOperator<BigDecimal>> OPERATORS = Stream.of(
            new AbstractMap.SimpleEntry<>('*', (BinaryOperator<BigDecimal>) BigDecimal::multiply),
            new AbstractMap.SimpleEntry<>('/', (BinaryOperator<BigDecimal>) (o1, o2) -> o1.divide(o2, SCALE, HALF_UP))
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    static final CompositeEstimator INSTANCE = new CompositeEstimator();

    private CompositeEstimator() {
    }

    @Override
    public Estimation estimate(Context context, Request request) {
        return BAIL;
    }

    @VisibleForTesting
    Estimation estimate(Context context, Request request, BiFunction<Context, Key, Estimation> function) {

        Map<String, Set<String>> composites = request.getEstimatorComposites();

        if (MapUtils.isEmpty(composites)) {
            return BAIL;
        }

        Key.KeyBuilder builder = Key.build(Key.from(request));

        BigDecimal price = ONE;

        BigDecimal confidence = ONE;

        for (Map.Entry<String, Set<String>> entry : composites.entrySet()) {

            String site = StringUtils.trimToEmpty(entry.getKey());

            if (site.length() <= 1) {
                return BAIL;
            }

            BinaryOperator<BigDecimal> operator = OPERATORS.get(site.charAt(0));

            if (operator == null) {
                return BAIL;
            }

            for (String instrument : entry.getValue()) {

                Key elementKey = builder.site(site.substring(1)).instrument(instrument).build();

                Estimation estimation = function.apply(context, elementKey);

                if (estimation == null || estimation.getPrice() == null || estimation.getConfidence() == null) {
                    return BAIL;
                }

                price = operator.apply(price, estimation.getPrice());

                confidence = confidence.multiply(estimation.getConfidence());

            }

        }

        return Estimation.builder().price(price).confidence(confidence).build();

    }

    public static class CompositeMidEstimator extends MidEstimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeLastEstimator extends LastEstimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

}
