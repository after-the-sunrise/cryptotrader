package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface Service extends Supplier<String> {

    String WILDCARD = "*";

    int SCALE = 10;

    BigDecimal HALF = new BigDecimal("0.5");

    BigDecimal SATOSHI = new BigDecimal("0.00000001");

    default NavigableMap<Instant, BigDecimal> collapsePrices(List<Trade> values,
                                                             Duration interval, Instant from, Instant to) {

        NavigableMap<Instant, BigDecimal[]> collapsed = new TreeMap<>();

        for (long i = from.toEpochMilli(); i < to.toEpochMilli(); i += interval.toMillis()) {

            Instant instant = Instant.ofEpochMilli(i);

            collapsed.put(instant, new BigDecimal[2]); // [size, notional]

        }

        Optional.ofNullable(values).orElse(Collections.emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(t -> t.getTimestamp() != null)
                .filter(t -> t.getTimestamp().isAfter(from.minus(interval)))
                .filter(t -> t.getTimestamp().isBefore(to))
                .filter(t -> t.getPrice() != null)
                .filter(t -> t.getSize() != null).forEach(t -> {

            Instant timestamp = t.getTimestamp();

            Map.Entry<Instant, BigDecimal[]> entry = collapsed.ceilingEntry(timestamp);

            if (entry == null) {
                return;
            }

            BigDecimal[] elements = entry.getValue();
            elements[0] = (elements[0] == null ? ZERO : elements[0]).add(t.getSize());
            elements[1] = (elements[1] == null ? ZERO : elements[1]).add(t.getSize().multiply(t.getPrice()));

        });

        NavigableMap<Instant, BigDecimal> prices = new TreeMap<>();

        BigDecimal previous = null;

        for (Map.Entry<Instant, BigDecimal[]> entry : collapsed.entrySet()) {

            BigDecimal[] elements = entry.getValue();

            BigDecimal current = previous;

            if (elements[0] != null && elements[0].signum() != 0) {
                current = elements[1].divide(elements[0], SCALE, HALF_UP);
            }

            prices.put(entry.getKey(), current);

            previous = current;

        }

        return prices;

    }

    default NavigableMap<Instant, BigDecimal> calculateReturns(SortedMap<Instant, BigDecimal> prices) {

        if (prices == null) {
            return Collections.emptyNavigableMap();
        }

        NavigableMap<Instant, BigDecimal> returns = new TreeMap<>();

        List<Map.Entry<Instant, BigDecimal>> entries = new ArrayList<>(prices.entrySet());

        for (int i = 1; i < entries.size(); i++) {

            BigDecimal p0 = entries.get(i - 1).getValue();

            BigDecimal p1 = entries.get(i).getValue();

            BigDecimal value = null;

            if (p0 != null && p1 != null) {

                double diff = Math.log(p1.doubleValue() / p0.doubleValue());

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
