package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.google.common.annotations.VisibleForTesting;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class SiteEstimator extends AbstractEstimator {

    @VisibleForTesting
    static final SiteEstimator INSTANCE = new SiteEstimator();

    private SiteEstimator() {
    }

    @Override
    public Context.Key getKey(Context context, Request request) {

        Context.Key key = Context.Key.from(request);

        CurrencyType structure = context.getInstrumentCurrency(key);

        CurrencyType funding = context.getFundingCurrency(key);

        Context.Key siteKey = Context.Key.build(key).instrument(WILDCARD).build();

        String instrument = context.findProduct(siteKey, structure, funding);

        return Context.Key.build(siteKey).instrument(instrument).build();

    }

    @Override
    public Estimation estimate(Context context, Request request) {
        return BAIL;
    }

    public static class SiteLastEstimator extends LastEstimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteMicroEstimator extends MicroEstimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteMidEstimator extends MidEstimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteUnivariateEstimator extends UnivariateEstimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteUnivariate005Estimator extends UnivariateEstimator.Univariate005Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteUnivariate010Estimator extends UnivariateEstimator.Univariate010Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteUnivariate015Estimator extends UnivariateEstimator.Univariate015Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteUnivariate020Estimator extends UnivariateEstimator.Univariate020Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteUnivariate030Estimator extends UnivariateEstimator.Univariate030Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteUnivariate045Estimator extends UnivariateEstimator.Univariate045Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteUnivariate060Estimator extends UnivariateEstimator.Univariate060Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteUnivariate120Estimator extends UnivariateEstimator.Univariate120Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteUnivariate240Estimator extends UnivariateEstimator.Univariate240Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteUnivariate360Estimator extends UnivariateEstimator.Univariate360Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteUnivariate480Estimator extends UnivariateEstimator.Univariate480Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteUnivariate720Estimator extends UnivariateEstimator.Univariate720Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteVwapEstimator extends VwapEstimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteVwap001Estimator extends VwapEstimator.Vwap001Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteVwap003Estimator extends VwapEstimator.Vwap003Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteVwap005Estimator extends VwapEstimator.Vwap005Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteVwap010Estimator extends VwapEstimator.Vwap010Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteVwap015Estimator extends VwapEstimator.Vwap015Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteVwap030Estimator extends VwapEstimator.Vwap030Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteVwap060Estimator extends VwapEstimator.Vwap060Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteVwap120Estimator extends VwapEstimator.Vwap120Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteVwap240Estimator extends VwapEstimator.Vwap240Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteVwap480Estimator extends VwapEstimator.Vwap480Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteVwap960Estimator extends VwapEstimator.Vwap960Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteDepthEstimator extends DepthEstimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteDepth001Estimator extends DepthEstimator.Depth001Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteDepth003Estimator extends DepthEstimator.Depth003Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteDepth005Estimator extends DepthEstimator.Depth005Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteDepth010Estimator extends DepthEstimator.Depth010Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteDepth015Estimator extends DepthEstimator.Depth015Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteDepth020Estimator extends DepthEstimator.Depth020Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteDepth030Estimator extends DepthEstimator.Depth030Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteDepth045Estimator extends DepthEstimator.Depth045Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteDepth060Estimator extends DepthEstimator.Depth060Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteDepth120Estimator extends DepthEstimator.Depth120Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteDepth240Estimator extends DepthEstimator.Depth240Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteDepth360Estimator extends DepthEstimator.Depth360Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteDepth480Estimator extends DepthEstimator.Depth480Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

    public static class SiteDepth720Estimator extends DepthEstimator.Depth720Estimator {
        @Override
        public Context.Key getKey(Context context, Request request) {
            return INSTANCE.getKey(context, request);
        }
    }

}
