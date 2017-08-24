package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.entity.Execution;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.ZonedDateTime;

import static java.math.BigDecimal.TEN;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitflyerTradeTest {

    private BitflyerTrade target;

    private Execution execution;

    @BeforeMethod
    public void setUp() throws Exception {

        execution = mock(Execution.class);

        target = new BitflyerTrade(execution);

    }

    @Test
    public void testGetTimestamp() throws Exception {

        when(execution.getTimestamp()).thenReturn(null);
        assertNull(target.getTimestamp());

        ZonedDateTime time = ZonedDateTime.now();
        when(execution.getTimestamp()).thenReturn(time);
        assertEquals(target.getTimestamp(), time.toInstant());

    }

    @Test
    public void testGetPrice() throws Exception {

        when(execution.getPrice()).thenReturn(null);
        assertNull(target.getPrice());

        when(execution.getPrice()).thenReturn(TEN);
        assertEquals(target.getPrice(), TEN);

    }

    @Test
    public void testGetSize() throws Exception {

        when(execution.getSize()).thenReturn(null);
        assertNull(target.getSize());

        when(execution.getSize()).thenReturn(TEN);
        assertEquals(target.getSize(), TEN);

    }

    @Test
    public void testGetBuyOrderId() throws Exception {

        when(execution.getBuyOrderId()).thenReturn(null);
        assertNull(target.getBuyOrderId());

        when(execution.getBuyOrderId()).thenReturn("test");
        assertEquals(target.getBuyOrderId(), "test");

    }

    @Test
    public void testGetSellOrderId() throws Exception {

        when(execution.getSellOrderId()).thenReturn(null);
        assertNull(target.getSellOrderId());

        when(execution.getSellOrderId()).thenReturn("test");
        assertEquals(target.getSellOrderId(), "test");

    }

    @Test
    public void testToString() throws Exception {

        doReturn("mock").when(execution).toString();

        assertEquals(target.toString(), "BitflyerTrade(delegate=mock)");

    }

}
