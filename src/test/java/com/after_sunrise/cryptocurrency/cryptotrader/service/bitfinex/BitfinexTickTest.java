package com.after_sunrise.cryptocurrency.cryptotrader.service.bitfinex;

import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitfinexTickTest {

    @Test
    public void test() {

        BitfinexTick target = BitfinexTick.builder()
                .ask(BigDecimal.valueOf(1))
                .bid(BigDecimal.valueOf(2))
                .last(BigDecimal.valueOf(3))
                .build();

        assertEquals(target.getAsk(), BigDecimal.valueOf(1));
        assertEquals(target.getBid(), BigDecimal.valueOf(2));
        assertEquals(target.getLast(), BigDecimal.valueOf(3));

        assertEquals(target.toString(), "BitfinexTick(last=3, ask=1, bid=2)");

    }

}
