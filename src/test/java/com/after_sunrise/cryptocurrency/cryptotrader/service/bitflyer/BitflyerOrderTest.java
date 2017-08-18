package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType;
import com.after_sunrise.cryptocurrency.bitflyer4j.core.StateType;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.OrderList;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitflyerOrderTest {

    private BitflyerOrder target;

    private OrderList.Response delegate;

    @BeforeMethod
    public void setUp() throws Exception {

        delegate = Mockito.mock(OrderList.Response.class);

        target = spy(new BitflyerOrder(delegate));

    }

    @Test
    public void testToString() throws Exception {
        assertNotNull(target.toString());
    }

    @Test
    public void testGetId() throws Exception {

        assertNull(target.getId());

        when(delegate.getAcceptanceId()).thenReturn("test");

        assertEquals(target.getId(), "test");

    }

    @Test
    public void testGetProduct() throws Exception {

        assertNull(target.getProduct());

        when(delegate.getProduct()).thenReturn("test");

        assertEquals(target.getProduct(), "test");

    }

    @Test
    public void testGetActive() throws Exception {

        when(delegate.getState()).thenReturn(null);
        assertFalse(target.getActive());

        when(delegate.getState()).thenReturn(StateType.EXPIRED);
        assertFalse(target.getActive());

        when(delegate.getState()).thenReturn(StateType.ACTIVE);
        assertTrue(target.getActive());

    }

    @Test
    public void testGetOrderPrice() throws Exception {

        assertNull(target.getOrderPrice());

        when(delegate.getPrice()).thenReturn(TEN);

        assertEquals(target.getOrderPrice(), TEN);

    }

    @Test
    public void testGetOrderQuantity() throws Exception {

        doReturn(ONE).when(target).getFilledQuantity();
        doReturn(TEN).when(target).getRemainingQuantity();
        assertEquals(target.getOrderQuantity(), TEN.add(ONE));

        doReturn(ONE).when(target).getFilledQuantity();
        doReturn(null).when(target).getRemainingQuantity();
        assertNull(target.getOrderQuantity());

        doReturn(null).when(target).getFilledQuantity();
        doReturn(TEN).when(target).getRemainingQuantity();
        assertNull(target.getOrderQuantity());

    }

    @Test
    public void testGetFilledQuantity() throws Exception {

        when(delegate.getExecutedSize()).thenReturn(ONE);
        when(delegate.getSide()).thenReturn(SideType.BUY);
        assertEquals(target.getFilledQuantity(), ONE);

        when(delegate.getExecutedSize()).thenReturn(ONE);
        when(delegate.getSide()).thenReturn(SideType.SELL);
        assertEquals(target.getFilledQuantity(), ONE.negate());

        when(delegate.getExecutedSize()).thenReturn(null);
        when(delegate.getSide()).thenReturn(SideType.BUY);
        assertNull(target.getFilledQuantity());

        when(delegate.getExecutedSize()).thenReturn(ONE);
        when(delegate.getSide()).thenReturn(null);
        assertNull(target.getFilledQuantity());

    }

    @Test
    public void testGetRemainingQuantity() throws Exception {

        when(delegate.getOutstandingSize()).thenReturn(ONE);
        when(delegate.getSide()).thenReturn(SideType.BUY);
        assertEquals(target.getRemainingQuantity(), ONE);

        when(delegate.getOutstandingSize()).thenReturn(ONE);
        when(delegate.getSide()).thenReturn(SideType.SELL);
        assertEquals(target.getRemainingQuantity(), ONE.negate());

        when(delegate.getOutstandingSize()).thenReturn(null);
        when(delegate.getSide()).thenReturn(SideType.BUY);
        assertNull(target.getRemainingQuantity());

        when(delegate.getOutstandingSize()).thenReturn(ONE);
        when(delegate.getSide()).thenReturn(null);
        assertNull(target.getRemainingQuantity());

    }

}
