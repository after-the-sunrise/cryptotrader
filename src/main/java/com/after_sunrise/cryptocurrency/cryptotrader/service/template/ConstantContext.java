package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class ConstantContext extends TemplateContext {

    public static final String ID = "constant";

    private final Cache<String, Optional<BigDecimal>> cache;

    public ConstantContext() {

        super(ID);

        cache = CacheBuilder.newBuilder().maximumSize(Byte.MAX_VALUE).build();

    }

    @Override
    public void close() throws Exception {

        super.close();

        cache.invalidateAll();

    }

    @VisibleForTesting
    BigDecimal convertDecimal(Key key) {

        if (key == null || key.getInstrument() == null) {
            return null;
        }

        Optional<BigDecimal> value = cache.getIfPresent(key.getInstrument());

        if (value == null) {

            try {

                value = Optional.of(new BigDecimal(key.getInstrument()));

            } catch (Exception e) {

                value = Optional.empty();

            }

            cache.put(key.getInstrument(), value);

        }

        return value.orElse(null);

    }

    @Override
    public BigDecimal getBestAskPrice(Key key) {
        return convertDecimal(key);
    }

    @Override
    public BigDecimal getBestBidPrice(Key key) {
        return convertDecimal(key);
    }

    @Override
    public BigDecimal getBestAskSize(Key key) {
        return convertDecimal(key);
    }

    @Override
    public BigDecimal getBestBidSize(Key key) {
        return convertDecimal(key);
    }

    @Override
    public BigDecimal getMidPrice(Key key) {
        return convertDecimal(key);
    }

    @Override
    public BigDecimal getLastPrice(Key key) {
        return convertDecimal(key);
    }

}
