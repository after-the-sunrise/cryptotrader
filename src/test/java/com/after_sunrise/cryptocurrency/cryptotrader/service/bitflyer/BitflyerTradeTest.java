package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitflyerTradeTest {

    private BitflyerTrade target;

    @BeforeMethod
    public void setUp() throws Exception {

        Instant t = Instant.ofEpochSecond(1234567890);

        target = new BitflyerTrade(t, new BigDecimal("1234"), new BigDecimal("0.5"));

    }

    @Test
    public void testToString() throws Exception {

        assertEquals(target.toString(), "BitflyerTrade(" +
                "count=1, timestamp=2009-02-13T23:31:30Z, notional=617.0, volume=0.5)");

    }

    @Test
    public void testAccumulate() throws Exception {

        assertEquals(target.getTimestamp(), Instant.ofEpochSecond(1234567890));
        assertNull(target.getBuyOrderId());
        assertNull(target.getSellOrderId());

        // Notional = 617, Volume = 0.5
        assertEquals(target.getPrice(), new BigDecimal("1234.0000000000"));
        assertEquals(target.getSize(), new BigDecimal("0.5"));

        // Notional = 617 + 1000 * 0.1 = 717, Volume = 0.5 * 0.1 = 0.6
        target.accumulate(new BigDecimal("1000"), new BigDecimal("0.1"));
        assertEquals(target.getPrice(), new BigDecimal("1195.0000000000"));
        assertEquals(target.getSize(), new BigDecimal("0.6"));

        // Snapshot
        Trade snapshot = target.snapshot();
        assertEquals(snapshot.getPrice(), new BigDecimal("1195.0000000000"));
        assertEquals(snapshot.getSize(), new BigDecimal("0.6"));

        // Notional = 717 + 1234 * 0.3 = 1087.2, Volume = 0.6 * 0.3 = 0.9
        target.accumulate(new BigDecimal("1234"), new BigDecimal("0.3"));
        assertEquals(target.getPrice(), new BigDecimal("1208.0000000000"));
        assertEquals(target.getSize(), new BigDecimal("0.9"));

        // Notional = 1087.2, Volume = 0.9 - 0.9 = 0
        target.accumulate(new BigDecimal("0.0"), new BigDecimal("-0.9"));
        assertEquals(target.getPrice(), null);
        assertEquals(target.getSize(), new BigDecimal("0.0"));

        // Snapshot remains the same.
        assertEquals(snapshot.getPrice(), new BigDecimal("1195.0000000000"));
        assertEquals(snapshot.getSize(), new BigDecimal("0.6"));

    }

}
