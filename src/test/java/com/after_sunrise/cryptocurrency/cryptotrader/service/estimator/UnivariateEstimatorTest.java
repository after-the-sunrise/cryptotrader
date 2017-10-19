package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator.Estimation;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.math.BigDecimal.ONE;
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

    private SortedMap<Instant, BigDecimal> prices;

    @BeforeMethod
    public void setUp() throws Exception {

        target = spy(new UnivariateEstimator());

        context = mock(Context.class);

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
        when(context.listTrades(key, from)).thenReturn(trades);
        doReturn(prices).when(target).collapsePrices(trades, interval, from, now);

        Estimation estimation = target.estimate(context, request);
        assertEquals(estimation.getPrice().toPlainString(), "19682.3911085423");
        assertEquals(estimation.getConfidence().toPlainString(), "0.0035580603");

        // No trades
        doReturn(null).when(context).listTrades(any(), any());
        assertSame(target.estimate(context, request), AbstractEstimator.BAIL);

    }

    @Test
    public void testCalculate_Prices() throws Exception {

        double delta = ONE.movePointLeft(16).doubleValue();
        double[] results = target.calculate(prices);
        assertEquals(results.length, 5);
        assertEquals(results[UnivariateEstimator.I_COEFFICIENT], 0.0000002977698988, delta);
        assertEquals(results[UnivariateEstimator.I_INTERCEPT], -425518.5240344088500000, delta);
        assertEquals(results[UnivariateEstimator.I_CORRELATION], 0.8406209335624100, delta);
        assertEquals(results[UnivariateEstimator.I_DETERMINATION], 0.7066435539433377, delta);
        assertEquals(results[UnivariateEstimator.I_POINTS], 40, delta);

        double c = results[UnivariateEstimator.I_COEFFICIENT];
        double i = results[UnivariateEstimator.I_INTERCEPT];
        double x;

        x = parseDate("2017-04-14").toEpochMilli();
        assertEquals(c * x + i, 18782.6317789757160000, delta);

        x = parseDate("2017-06-01").toEpochMilli();
        assertEquals(c * x + i, 20017.5431032831550000, delta);

    }

    @Test
    public void testCalculate_Returns() throws Exception {

        double delta = ONE.movePointLeft(20).doubleValue();
        double[] results = target.calculate(target.calculateReturns(prices));
        assertEquals(results.length, 5);
        assertEquals(results[UnivariateEstimator.I_COEFFICIENT], 0.0000000000002841579457946422, delta);
        assertEquals(results[UnivariateEstimator.I_INTERCEPT], -0.4235512423247192, delta);
        assertEquals(results[UnivariateEstimator.I_CORRELATION], 0.05964947873647785, delta);
        assertEquals(results[UnivariateEstimator.I_DETERMINATION], 0.003558060313533523, delta);
        assertEquals(results[UnivariateEstimator.I_POINTS], 39, delta);

        double c = results[UnivariateEstimator.I_COEFFICIENT];
        double i = results[UnivariateEstimator.I_INTERCEPT];
        double x0, x1;

        x0 = prices.get(parseDate("2017-04-13")).doubleValue();
        x1 = parseDate("2017-04-14").toEpochMilli();
        assertEquals(Math.exp(c * x1 + i) * x0, 18434.941819572457, delta);

        x0 = prices.get(parseDate("2017-05-31")).doubleValue();
        x1 = parseDate("2017-06-01").toEpochMilli();
        assertEquals(Math.exp(c * x1 + i) * x0, 19682.39110854227, delta);

    }

}
