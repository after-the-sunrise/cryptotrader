package com.after_sunrise.cryptocurrency.cryptotrader.service.bitfinex;

import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitfinexTradeTest {

    @Test
    public void test() {

        BitfinexTrade target = BitfinexTrade.builder()
                .timestamp(Instant.ofEpochMilli(1))
                .price(BigDecimal.valueOf(2))
                .size(BigDecimal.valueOf(3))
                .buyOrderId("b")
                .sellOrderId("s")
                .build();

        assertEquals(target.getTimestamp(), Instant.ofEpochMilli(1));
        assertEquals(target.getPrice(), BigDecimal.valueOf(2));
        assertEquals(target.getSize(), BigDecimal.valueOf(3));
        assertEquals(target.getBuyOrderId(), "b");
        assertEquals(target.getSellOrderId(), "s");

        assertEquals(target.toString(),
                "BitfinexTrade(timestamp=1970-01-01T00:00:00.001Z, " +
                        "price=2, size=3, buyOrderId=b, sellOrderId=s)"
        );

    }

}
