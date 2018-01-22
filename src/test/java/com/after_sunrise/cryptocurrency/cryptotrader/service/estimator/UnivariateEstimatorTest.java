package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import org.apache.commons.configuration2.MapConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class UnivariateEstimatorTest {

    private UnivariateEstimator target;

    private Context context;

    private Map<String, Object> configuration;

    private SortedMap<Instant, BigDecimal> prices;

    @BeforeMethod
    public void setUp() throws Exception {

        configuration = new HashMap<>();

        context = mock(Context.class);

        target = spy(new UnivariateEstimator());

        target.setConfiguration(new MapConfiguration(configuration));

        prices = new TreeMap<>();
        prices.put(parseDate("2017-05-31"), new BigDecimal("19650.57"));
        prices.put(parseDate("2017-05-30"), new BigDecimal("19677.85"));
        prices.put(parseDate("2017-05-29"), new BigDecimal("19682.57"));
        prices.put(parseDate("2017-05-26"), new BigDecimal("19686.84"));
        prices.put(parseDate("2017-05-25"), new BigDecimal("19813.13"));
        prices.put(parseDate("2017-05-24"), new BigDecimal("19742.98"));
        prices.put(parseDate("2017-05-23"), new BigDecimal("19613.28"));
        prices.put(parseDate("2017-05-22"), new BigDecimal("19678.28"));
        prices.put(parseDate("2017-05-19"), new BigDecimal("19590.76"));
        prices.put(parseDate("2017-05-18"), new BigDecimal("19553.86"));
        prices.put(parseDate("2017-05-17"), new BigDecimal("19814.88"));
        prices.put(parseDate("2017-05-16"), new BigDecimal("19919.82"));
        prices.put(parseDate("2017-05-15"), new BigDecimal("19869.85"));
        prices.put(parseDate("2017-05-12"), new BigDecimal("19883.90"));
        prices.put(parseDate("2017-05-11"), new BigDecimal("19961.55"));
        prices.put(parseDate("2017-05-10"), new BigDecimal("19900.09"));
        prices.put(parseDate("2017-05-09"), new BigDecimal("19843.00"));
        prices.put(parseDate("2017-05-08"), new BigDecimal("19895.70"));
        prices.put(parseDate("2017-05-02"), new BigDecimal("19445.70"));
        prices.put(parseDate("2017-05-01"), new BigDecimal("19310.52"));
        prices.put(parseDate("2017-04-28"), new BigDecimal("19196.74"));
        prices.put(parseDate("2017-04-27"), new BigDecimal("19251.87"));
        prices.put(parseDate("2017-04-26"), new BigDecimal("19289.43"));
        prices.put(parseDate("2017-04-25"), new BigDecimal("19079.33"));
        prices.put(parseDate("2017-04-24"), new BigDecimal("18875.88"));
        prices.put(parseDate("2017-04-21"), new BigDecimal("18620.75"));
        prices.put(parseDate("2017-04-20"), new BigDecimal("18430.49"));
        prices.put(parseDate("2017-04-19"), new BigDecimal("18432.20"));
        prices.put(parseDate("2017-04-18"), new BigDecimal("18418.59"));
        prices.put(parseDate("2017-04-17"), new BigDecimal("18355.26"));
        prices.put(parseDate("2017-04-14"), new BigDecimal("18335.63"));
        prices.put(parseDate("2017-04-13"), new BigDecimal("18426.84"));
        prices.put(parseDate("2017-04-12"), new BigDecimal("18552.61"));
        prices.put(parseDate("2017-04-11"), new BigDecimal("18747.87"));
        prices.put(parseDate("2017-04-10"), new BigDecimal("18797.88"));
        prices.put(parseDate("2017-04-07"), new BigDecimal("18664.63"));
        prices.put(parseDate("2017-04-06"), new BigDecimal("18597.06"));
        prices.put(parseDate("2017-04-05"), new BigDecimal("18861.27"));
        prices.put(parseDate("2017-04-04"), new BigDecimal("18810.25"));
        prices.put(parseDate("2017-04-03"), new BigDecimal("18983.23"));

    }

    private Instant parseDate(String ymd) {

        LocalDate date = LocalDate.parse(ymd, DateTimeFormatter.ISO_DATE);

        ZonedDateTime time = date.atStartOfDay(ZoneId.of("Asia/Tokyo"));

        return time.toInstant();

    }

    @Test
    public void testEstimate() throws Exception {

        Instant now = parseDate("2017-05-31");
        Instant est = parseDate("2017-06-01");
        Duration interval = Duration.between(now, est);
        Instant from = parseDate("2017-04-01");

        Request request = Request.builder().currentTime(now).targetTime(est).build();
        Key key = Key.from(request);

        List<Trade> trades = Collections.singletonList(mock(Trade.class));
        when(context.listTrades(key, from.minus(interval))).thenReturn(trades);
        doReturn(prices).when(target).collapsePrices(trades, interval, from, now, false);

        Estimation estimation = target.estimate(context, request);
        assertEquals(estimation.getPrice().toPlainString(), "19682.3911085423");
        assertEquals(estimation.getConfidence().toPlainString(), "0.0035580603");

        // Not enough samples
        configuration.put(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.UnivariateEstimator.samples",
                prices.size() * 2
        );
        assertSame(target.estimate(context, request), AbstractEstimator.BAIL);

        // No trades
        doReturn(null).when(context).listTrades(any(), any());
        assertSame(target.estimate(context, request), AbstractEstimator.BAIL);

    }

    @Test
    public void testGetSamples() {

        assertEquals(target.getSamples(), 60);

        configuration.put(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.estimator.UnivariateEstimator.samples",
                64
        );
        assertEquals(target.getSamples(), 64);

        assertEquals(new UnivariateEstimator.Univariate010Estimator().getSamples(), 10);
        assertEquals(new UnivariateEstimator.Univariate015Estimator().getSamples(), 15);
        assertEquals(new UnivariateEstimator.Univariate020Estimator().getSamples(), 20);
        assertEquals(new UnivariateEstimator.Univariate030Estimator().getSamples(), 30);
        assertEquals(new UnivariateEstimator.Univariate045Estimator().getSamples(), 45);
        assertEquals(new UnivariateEstimator.Univariate060Estimator().getSamples(), 60);
        assertEquals(new UnivariateEstimator.Univariate120Estimator().getSamples(), 120);
        assertEquals(new UnivariateEstimator.Univariate240Estimator().getSamples(), 240);
        assertEquals(new UnivariateEstimator.Univariate360Estimator().getSamples(), 360);
        assertEquals(new UnivariateEstimator.Univariate480Estimator().getSamples(), 480);
        assertEquals(new UnivariateEstimator.Univariate720Estimator().getSamples(), 720);

    }

}
