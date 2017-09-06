package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.TradeExecution;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitflyerExecutionTest {

    private BitflyerExecution target;

    private TradeExecution delegate;

    @BeforeMethod
    public void setUp() throws Exception {

        delegate = Mockito.mock(TradeExecution.class);

        target = spy(new BitflyerExecution(delegate));

    }

    @Test
    public void testToString() throws Exception {
        assertNotNull(target.toString());
    }

    @Test
    public void testGetId() throws Exception {

        when(delegate.getId()).thenReturn(null);
        assertNull(target.getId());

        when(delegate.getId()).thenReturn(123L);
        assertEquals(target.getId(), "123");

    }

    @Test
    public void testGetOrderId() throws Exception {

        assertNull(target.getOrderId());

        when(delegate.getAcceptanceId()).thenReturn("X");

        assertEquals(target.getOrderId(), "X");

    }

    @Test
    public void testGetTime() throws Exception {

        ZonedDateTime value = ZonedDateTime.now();

        assertNull(target.getTime());

        when(delegate.getExecDate()).thenReturn(value);

        assertEquals(target.getTime(), value.toInstant());

    }

    @Test
    public void testGetPrice() throws Exception {

        BigDecimal value = new BigDecimal("12.345");

        assertNull(target.getPrice());

        when(delegate.getPrice()).thenReturn(value);

        assertEquals(target.getPrice(), value);

    }

    @Test
    public void testGetSize() throws Exception {

        BigDecimal value = null;
        when(delegate.getSide()).thenReturn(null);
        when(delegate.getSize()).thenReturn(value);
        assertNull(target.getSize());

        // With Price

        value = new BigDecimal("12.345");
        when(delegate.getSize()).thenReturn(value);

        when(delegate.getSide()).thenReturn(null);
        assertNull(target.getSize());

        when(delegate.getSide()).thenReturn(SideType.BUY);
        assertEquals(target.getSize(), value);

        when(delegate.getSide()).thenReturn(SideType.SELL);
        assertEquals(target.getSize(), value.negate());

        // Without Price

        value = null;
        when(delegate.getSize()).thenReturn(value);

        when(delegate.getSide()).thenReturn(null);
        assertNull(target.getSize());

        when(delegate.getSide()).thenReturn(SideType.BUY);
        assertNull(target.getSize());

        when(delegate.getSide()).thenReturn(SideType.SELL);
        assertNull(target.getSize());

    }

}
