package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.core.Composite;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

import static java.math.BigDecimal.*;
import static java.math.RoundingMode.HALF_UP;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class CompositeEstimator extends AbstractEstimator {

    static final CompositeEstimator INSTANCE = new CompositeEstimator();

    private CompositeEstimator() {
    }

    @Override
    public Estimation estimate(Context context, Request request) {
        return BAIL;
    }

    @VisibleForTesting
    Estimation estimate(Context context, Request request, BiFunction<Context, Request, Estimation> function) {

        List<Composite> composites = request.getEstimatorComposites();

        if (CollectionUtils.isEmpty(composites)) {
            return BAIL;
        }

        Request.RequestBuilder builder = Request.build(request);

        BigDecimal[] prices = new BigDecimal[1];

        BigDecimal[] confidences = new BigDecimal[1];

        for (Composite composite : composites) {

            String site = StringUtils.trimToEmpty(composite.getSite());

            if (site.length() <= 1) {
                return BAIL;
            }

            char operation = site.charAt(0);

            BinaryOperator<BigDecimal> operator = null;

            if ('+' == operation) {
                operator = BigDecimal::add;
            }

            if ('-' == operation) {
                operator = BigDecimal::subtract;
            }

            if ('*' == operation) {
                operator = BigDecimal::multiply;
            }

            if ('/' == operation) {
                operator = (o1, o2) -> o1.divide(o2, SCALE, HALF_UP);
            }

            if ('@' != operation && operator == null) {
                return BAIL;
            }

            String instrument = composite.getInstrument();

            Request elementRequest = builder.site(site.substring(1)).instrument(instrument).build();

            Estimation estimation = function.apply(context, elementRequest);

            if (estimation == null || estimation.getPrice() == null || estimation.getConfidence() == null) {
                return BAIL;
            }

            if (operator != null) {

                prices[0] = operator.apply(trim(prices[0], ONE), estimation.getPrice());

                confidences[0] = trim(confidences[0], ONE).multiply(estimation.getConfidence());

            } else {

                prices = ArrayUtils.add(prices, estimation.getPrice());

                confidences = ArrayUtils.add(confidences, estimation.getConfidence());

            }

        }

        long countPrice = Stream.of(prices).filter(Objects::nonNull).count();
        BigDecimal totalPrice = Stream.of(prices).filter(Objects::nonNull).reduce(BigDecimal::add).orElse(ZERO);
        BigDecimal averagePrice = totalPrice.divide(valueOf(countPrice), SCALE, HALF_UP);

        long countConfidence = Stream.of(confidences).filter(Objects::nonNull).count();
        BigDecimal totalConfidence = Stream.of(confidences).filter(Objects::nonNull).reduce(BigDecimal::add).orElse(ONE);
        BigDecimal averageConfidence = totalConfidence.divide(valueOf(countConfidence), SCALE, HALF_UP);

        return Estimation.builder().price(averagePrice).confidence(averageConfidence).build();

    }

    public static class CompositeLastEstimator extends LastEstimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeMicroEstimator extends MicroEstimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeMidEstimator extends MidEstimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeUnivariateEstimator extends UnivariateEstimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeUnivariate005Estimator extends UnivariateEstimator.Univariate005Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeUnivariate010Estimator extends UnivariateEstimator.Univariate010Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeUnivariate015Estimator extends UnivariateEstimator.Univariate015Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeUnivariate020Estimator extends UnivariateEstimator.Univariate020Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeUnivariate030Estimator extends UnivariateEstimator.Univariate030Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeUnivariate045Estimator extends UnivariateEstimator.Univariate045Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeUnivariate060Estimator extends UnivariateEstimator.Univariate060Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeUnivariate120Estimator extends UnivariateEstimator.Univariate120Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeUnivariate240Estimator extends UnivariateEstimator.Univariate240Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeUnivariate360Estimator extends UnivariateEstimator.Univariate360Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeUnivariate480Estimator extends UnivariateEstimator.Univariate480Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeUnivariate720Estimator extends UnivariateEstimator.Univariate720Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeVwapEstimator extends VwapEstimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeVwap001Estimator extends VwapEstimator.Vwap001Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeVwap003Estimator extends VwapEstimator.Vwap003Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeVwap005Estimator extends VwapEstimator.Vwap005Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeVwap010Estimator extends VwapEstimator.Vwap010Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeVwap015Estimator extends VwapEstimator.Vwap015Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeVwap030Estimator extends VwapEstimator.Vwap030Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeVwap060Estimator extends VwapEstimator.Vwap060Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeVwap120Estimator extends VwapEstimator.Vwap120Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeVwap240Estimator extends VwapEstimator.Vwap240Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeVwap480Estimator extends VwapEstimator.Vwap480Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeVwap960Estimator extends VwapEstimator.Vwap960Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeDepthEstimator extends DepthEstimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeDepth001Estimator extends DepthEstimator.Depth001Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeDepth003Estimator extends DepthEstimator.Depth003Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeDepth005Estimator extends DepthEstimator.Depth005Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeDepth010Estimator extends DepthEstimator.Depth010Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeDepth015Estimator extends DepthEstimator.Depth015Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeDepth020Estimator extends DepthEstimator.Depth020Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeDepth030Estimator extends DepthEstimator.Depth030Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeDepth045Estimator extends DepthEstimator.Depth045Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeDepth060Estimator extends DepthEstimator.Depth060Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeDepth120Estimator extends DepthEstimator.Depth120Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeDepth240Estimator extends DepthEstimator.Depth240Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeDepth360Estimator extends DepthEstimator.Depth360Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeDepth480Estimator extends DepthEstimator.Depth480Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

    public static class CompositeDepth720Estimator extends DepthEstimator.Depth720Estimator {
        @Override
        public Estimation estimate(Context context, Request request) {
            return INSTANCE.estimate(context, request, super::estimate);
        }
    }

}
