package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateAdviser;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static java.math.RoundingMode.UP;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class BitflyerAdviser extends TemplateAdviser implements BitflyerService {

    private static final double SWAP_RATE = 0.0004;

    public BitflyerAdviser() {
        super(ID);
    }

    @Override
    protected BigDecimal calculateBasis(Context context, Request request) {

        BigDecimal base = super.calculateBasis(context, request);

        if (base == null) {
            return null;
        }

        Instant now = request.getCurrentTime();

        if (now == null) {
            return base;
        }

        ZonedDateTime expiry = context.getExpiry(Context.Key.from(request));

        if (expiry == null) {
            return base; // Not an expiry product.
        }

        ZonedDateTime sod = expiry.truncatedTo(ChronoUnit.DAYS);

        Duration swapFree = Duration.between(sod, expiry);

        Duration maturity = Duration.between(request.getCurrentTime(), expiry);

        if (maturity.compareTo(swapFree) < 0) {
            return base; // Expiring without swap.
        }

        long swaps = maturity.toDays();

        double rate = Math.pow(1 + SWAP_RATE, swaps) - 1;

        return base.add(BigDecimal.valueOf(rate)).setScale(SCALE, UP);

    }

}
