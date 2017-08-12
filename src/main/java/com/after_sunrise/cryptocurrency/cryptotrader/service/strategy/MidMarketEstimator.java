package com.after_sunrise.cryptocurrency.cryptotrader.service.strategy;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.MarketEstimator;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;

import static java.math.BigDecimal.ONE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class MidMarketEstimator implements MarketEstimator {

    private static final BigDecimal HALF = new BigDecimal("0.5");

    private static final Estimation BAIL = Estimation.builder().build();

    @Override
    public Estimation estimate(Context context, Request request) {

        String site = request.getSite();

        String instrument = request.getInstrument();

        Instant timestamp = request.getTimestamp();

        BigDecimal ask = context.getBesAskPrice(site, instrument, timestamp);

        if (ask == null || ask.signum() == 0) {

            log.trace("Bailing : Ask = {}", ask);

            return BAIL;

        }

        BigDecimal bid = context.getBesBidPrice(site, instrument, timestamp);

        if (bid == null || bid.signum() == 0) {

            log.trace("Bailing : Bid = {}", bid);

            return BAIL;

        }

        BigDecimal mid = ask.add(bid).multiply(HALF);

        return Estimation.builder().price(mid).confidence(ONE).build();

    }

}
