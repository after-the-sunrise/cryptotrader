package com.after_sunrise.cryptocurrency.cryptotrader.service.oanda;

import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class OandaTickTest {

    @Test
    public void test() {

        OandaTick target = OandaTick.builder()
                .ask(BigDecimal.valueOf(1))
                .bid(BigDecimal.valueOf(2))
                .build();

        assertEquals(target.getAsk(), BigDecimal.valueOf(1));
        assertEquals(target.getBid(), BigDecimal.valueOf(2));

        assertEquals(target.toString(), "OandaTick(ask=1, bid=2)");

    }

}
