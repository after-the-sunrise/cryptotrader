package com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck;

import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class CoincheckTradeTest {

    @Test
    public void test() {

        CoincheckTrade target = CoincheckTrade.builder()
                .id(0L)
                .timestamp(Instant.ofEpochMilli(1))
                .price(BigDecimal.valueOf(2))
                .size(BigDecimal.valueOf(3))
                .build();

        assertEquals(target.getId(), (Long) 0L);
        assertEquals(target.getTimestamp(), Instant.ofEpochMilli(1));
        assertEquals(target.getPrice(), BigDecimal.valueOf(2));
        assertEquals(target.getSize(), BigDecimal.valueOf(3));

        assertEquals(target.toString(),
                "CoincheckTrade(id=0, timestamp=1970-01-01T00:00:00.001Z, price=2, size=3)"
        );

    }

}
