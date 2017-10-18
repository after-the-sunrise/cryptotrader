package com.after_sunrise.cryptocurrency.cryptotrader.service.estimator;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public abstract class AbstractEstimator implements Estimator {

    protected static final Estimation BAIL = Estimation.builder().confidence(ZERO).build();

    protected Key getKey(Request request) {
        return Key.from(request);
    }

    protected NavigableMap<Instant, BigDecimal> calculateReturns(List<Trade> values,
                                                                 Duration interval, Instant from, Instant to) {

        NavigableMap<Instant, double[]> collapsed = new TreeMap<>();

        for (long i = from.toEpochMilli(); i < to.toEpochMilli(); i += interval.toMillis()) {

            Instant instant = Instant.ofEpochMilli(i);

            collapsed.put(instant, new double[3]); // [count, size, notional]

        }

        Optional.ofNullable(values).orElse(Collections.emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(t -> t.getTimestamp() != null)
                .filter(t -> t.getTimestamp().isAfter(from))
                .filter(t -> t.getTimestamp().isBefore(to))
                .filter(t -> t.getPrice() != null)
                .filter(t -> t.getSize() != null).forEach(t -> {

            Instant timestamp = t.getTimestamp();

            Entry<Instant, double[]> entry = collapsed.ceilingEntry(timestamp);

            if (entry == null) {
                return;
            }

            double[] elements = entry.getValue();

            elements[0] = elements[0] + 1;
            elements[1] = elements[1] + t.getSize().doubleValue();
            elements[2] = elements[2] + t.getSize().doubleValue() * t.getPrice().doubleValue();

        });

        NavigableMap<Instant, Double> prices = new TreeMap<>();

        Double previous = null;

        for (Entry<Instant, double[]> entry : collapsed.entrySet()) {

            double[] elements = entry.getValue();

            Double current = elements[0] == 0 ? previous : Double.valueOf(elements[2] / elements[1]);

            prices.put(entry.getKey(), current);

            previous = current;

        }

        NavigableMap<Instant, BigDecimal> returns = new TreeMap<>();

        List<Entry<Instant, Double>> entries = new ArrayList<>(prices.entrySet());

        for (int i = 1; i < entries.size(); i++) {

            Double p0 = entries.get(i - 1).getValue();

            Double p1 = entries.get(i).getValue();

            BigDecimal value = null;

            if (p0 != null) {

                double diff = Math.log(p1 / p0);

                if (Double.isFinite(diff)) {

                    value = BigDecimal.valueOf(diff);

                    value = value.setScale(SCALE, HALF_UP);

                }

            }

            returns.put(entries.get(i).getKey(), value);

        }

        return returns;

    }

}
