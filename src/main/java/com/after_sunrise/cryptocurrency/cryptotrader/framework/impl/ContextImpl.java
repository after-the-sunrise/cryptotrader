package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class ContextImpl extends AbstractService implements Context {

    private final Map<String, Context> contexts;

    @Inject
    public ContextImpl(Injector injector) {

        contexts = injector.getInstance(ServiceFactory.class).loadMap(Context.class);

    }

    @Override
    public void close() throws Exception {

        Exception exception = null;

        for (Context context : contexts.values()) {

            try {

                context.close();

            } catch (Exception e) {

                if (exception == null) {
                    exception = new Exception("Failed to close context(s).");
                }

                exception.addSuppressed(e);

            }

        }

        if (exception != null) {
            throw exception;
        }

    }

    @Override
    public String get() {
        return WILDCARD;
    }

    @VisibleForTesting
    <R> R forContext(Key key, Function<Context, R> function) {

        if (key == null) {
            return null;
        }

        Context context = contexts.get(key.getSite());

        if (context == null) {
            return null;
        }

        return function.apply(context);

    }

    @Override
    public StateType getState(Key key) {
        return forContext(key, c -> c.getState(key));
    }

    @Override
    public BigDecimal getBestAskPrice(Key key) {
        return forContext(key, c -> c.getBestAskPrice(key));
    }

    @Override
    public BigDecimal getBestBidPrice(Key key) {
        return forContext(key, c -> c.getBestBidPrice(key));
    }

    @Override
    public BigDecimal getBestAskSize(Key key) {
        return forContext(key, c -> c.getBestAskSize(key));
    }

    @Override
    public BigDecimal getBestBidSize(Key key) {
        return forContext(key, c -> c.getBestBidSize(key));
    }

    @Override
    public BigDecimal getMidPrice(Key key) {
        return forContext(key, c -> c.getMidPrice(key));
    }

    @Override
    public BigDecimal getLastPrice(Key key) {
        return forContext(key, c -> c.getLastPrice(key));
    }

    @Override
    public Map<BigDecimal, BigDecimal> getAskPrices(Key key) {
        return forContext(key, c -> c.getAskPrices(key));
    }

    @Override
    public Map<BigDecimal, BigDecimal> getBidPrices(Key key) {
        return forContext(key, c -> c.getBidPrices(key));
    }

    @Override
    public List<Trade> listTrades(Key key, Instant fromTime) {
        return forContext(key, c -> c.listTrades(key, fromTime));
    }

    @Override
    public CurrencyType getInstrumentCurrency(Key key) {
        return forContext(key, c -> c.getInstrumentCurrency(key));
    }

    @Override
    public CurrencyType getFundingCurrency(Key key) {
        return forContext(key, c -> c.getFundingCurrency(key));
    }

    @Override
    public String findProduct(Key key, CurrencyType instrument, CurrencyType funding) {
        return forContext(key, c -> c.findProduct(key, instrument, funding));
    }

    @Override
    public BigDecimal getConversionPrice(Key key, CurrencyType currency) {
        return forContext(key, c -> c.getConversionPrice(key, currency));
    }

    @Override
    public BigDecimal getInstrumentPosition(Key key) {
        return forContext(key, c -> c.getInstrumentPosition(key));
    }

    @Override
    public BigDecimal getFundingPosition(Key key) {
        return forContext(key, c -> c.getFundingPosition(key));
    }

    @Override
    public BigDecimal roundLotSize(Key key, BigDecimal value, RoundingMode mode) {
        return forContext(key, c -> c.roundLotSize(key, value, mode));
    }

    @Override
    public BigDecimal roundTickSize(Key key, BigDecimal value, RoundingMode mode) {
        return forContext(key, c -> c.roundTickSize(key, value, mode));
    }

    @Override
    public BigDecimal getCommissionRate(Key key) {
        return forContext(key, c -> c.getCommissionRate(key));
    }

    @Override
    public Boolean isMarginable(Key key) {
        return forContext(key, c -> c.isMarginable(key));
    }

    @Override
    public ZonedDateTime getExpiry(Key key) {
        return forContext(key, c -> c.getExpiry(key));
    }

    @Override
    public Order findOrder(Key key, String id) {
        return forContext(key, c -> c.findOrder(key, id));
    }

    @Override
    public List<Order> listActiveOrders(Key key) {
        return forContext(key, c -> c.listActiveOrders(key));
    }

    @Override
    public List<Order.Execution> listExecutions(Key key) {
        return forContext(key, c -> c.listExecutions(key));
    }

    @Override
    public Map<CreateInstruction, String> createOrders(Key key, Set<CreateInstruction> instructions) {
        return forContext(key, c -> c.createOrders(key, instructions));
    }

    @Override
    public Map<CancelInstruction, String> cancelOrders(Key key, Set<CancelInstruction> instructions) {
        return forContext(key, c -> c.cancelOrders(key, instructions));
    }

}
