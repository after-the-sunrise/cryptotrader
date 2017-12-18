package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType;
import com.after_sunrise.cryptocurrency.bitflyer4j.core.StateType;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.OrderList;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.ParentList;
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

    private BitflyerOrder.Child child;

    private OrderList dChild;

    private BitflyerOrder.Parent parent;

    private ParentList dParent;

    @BeforeMethod
    public void setUp() throws Exception {

        dChild = Mockito.mock(OrderList.class);
        child = spy(new BitflyerOrder.Child(dChild));

        dParent = Mockito.mock(ParentList.class);
        parent = spy(new BitflyerOrder.Parent(dParent));

    }

    @Test
    public void testToString() {

        assertNotNull(child.toString());

        assertNotNull(parent.toString());

    }

    @Test
    public void testAccept() {

        BitflyerOrder.Visitor<?> visitor = mock(BitflyerOrder.Visitor.class);
        doReturn("c").when(visitor).visit(any(BitflyerOrder.Child.class));
        doReturn("p").when(visitor).visit(any(BitflyerOrder.Parent.class));

        assertEquals("c", child.accept(visitor));
        assertEquals("p", parent.accept(visitor));

    }

    @Test
    public void testGetId() {

        assertNull(child.getId());
        when(dChild.getAcceptanceId()).thenReturn("test");
        assertEquals(child.getId(), "test");

        assertNull(parent.getId());
        when(dParent.getAcceptanceId()).thenReturn("test");
        assertEquals(parent.getId(), "test");

    }

    @Test
    public void testGetProduct() {

        assertNull(child.getProduct());
        when(dChild.getProduct()).thenReturn("test");
        assertEquals(child.getProduct(), "test");

        assertNull(parent.getProduct());
        when(dParent.getProduct()).thenReturn("test");
        assertEquals(parent.getProduct(), "test");

    }

    @Test
    public void testGetActive() {

        when(dChild.getState()).thenReturn(null);
        assertFalse(child.getActive());

        when(dChild.getState()).thenReturn(StateType.EXPIRED);
        assertFalse(child.getActive());

        when(dChild.getState()).thenReturn(StateType.ACTIVE);
        assertTrue(child.getActive());

        when(dParent.getState()).thenReturn(null);
        assertFalse(parent.getActive());

        when(dParent.getState()).thenReturn(StateType.EXPIRED);
        assertFalse(parent.getActive());

        when(dParent.getState()).thenReturn(StateType.ACTIVE);
        assertTrue(parent.getActive());

    }

    @Test
    public void testGetOrderPrice() {

        assertNull(child.getOrderPrice());
        when(dChild.getPrice()).thenReturn(TEN);
        assertEquals(child.getOrderPrice(), TEN);

        assertNull(parent.getOrderPrice());
        when(dParent.getPrice()).thenReturn(TEN);
        assertEquals(parent.getOrderPrice(), TEN);

    }

    @Test
    public void testGetOrderQuantity() {

        doReturn(ONE).when(child).getFilledQuantity();
        doReturn(TEN).when(child).getRemainingQuantity();
        assertEquals(child.getOrderQuantity(), TEN.add(ONE));

        doReturn(ONE).when(child).getFilledQuantity();
        doReturn(null).when(child).getRemainingQuantity();
        assertNull(child.getOrderQuantity());

        doReturn(null).when(child).getFilledQuantity();
        doReturn(TEN).when(child).getRemainingQuantity();
        assertNull(child.getOrderQuantity());

        doReturn(ONE).when(parent).getFilledQuantity();
        doReturn(TEN).when(parent).getRemainingQuantity();
        assertEquals(parent.getOrderQuantity(), TEN.add(ONE));

        doReturn(ONE).when(parent).getFilledQuantity();
        doReturn(null).when(parent).getRemainingQuantity();
        assertNull(parent.getOrderQuantity());

        doReturn(null).when(parent).getFilledQuantity();
        doReturn(TEN).when(parent).getRemainingQuantity();
        assertNull(parent.getOrderQuantity());

    }

    @Test
    public void testGetFilledQuantity() {

        when(dChild.getExecutedSize()).thenReturn(ONE);
        when(dChild.getSide()).thenReturn(SideType.BUY);
        assertEquals(child.getFilledQuantity(), ONE);

        when(dChild.getExecutedSize()).thenReturn(ONE);
        when(dChild.getSide()).thenReturn(SideType.SELL);
        assertEquals(child.getFilledQuantity(), ONE.negate());

        when(dChild.getExecutedSize()).thenReturn(null);
        when(dChild.getSide()).thenReturn(SideType.BUY);
        assertNull(child.getFilledQuantity());

        when(dChild.getExecutedSize()).thenReturn(ONE);
        when(dChild.getSide()).thenReturn(null);
        assertNull(child.getFilledQuantity());

        when(dParent.getExecutedSize()).thenReturn(ONE);
        when(dParent.getSide()).thenReturn(SideType.BUY);
        assertEquals(parent.getFilledQuantity(), ONE);

        when(dParent.getExecutedSize()).thenReturn(ONE);
        when(dParent.getSide()).thenReturn(SideType.SELL);
        assertEquals(parent.getFilledQuantity(), ONE.negate());

        when(dParent.getExecutedSize()).thenReturn(null);
        when(dParent.getSide()).thenReturn(SideType.BUY);
        assertNull(parent.getFilledQuantity());

        when(dParent.getExecutedSize()).thenReturn(ONE);
        when(dParent.getSide()).thenReturn(null);
        assertNull(parent.getFilledQuantity());

    }

    @Test
    public void testGetRemainingQuantity() {

        when(dChild.getOutstandingSize()).thenReturn(ONE);
        when(dChild.getSide()).thenReturn(SideType.BUY);
        assertEquals(child.getRemainingQuantity(), ONE);

        when(dChild.getOutstandingSize()).thenReturn(ONE);
        when(dChild.getSide()).thenReturn(SideType.SELL);
        assertEquals(child.getRemainingQuantity(), ONE.negate());

        when(dChild.getOutstandingSize()).thenReturn(null);
        when(dChild.getSide()).thenReturn(SideType.BUY);
        assertNull(child.getRemainingQuantity());

        when(dChild.getOutstandingSize()).thenReturn(ONE);
        when(dChild.getSide()).thenReturn(null);
        assertNull(child.getRemainingQuantity());

        when(dParent.getOutstandingSize()).thenReturn(ONE);
        when(dParent.getSide()).thenReturn(SideType.BUY);
        assertEquals(parent.getRemainingQuantity(), ONE);

        when(dParent.getOutstandingSize()).thenReturn(ONE);
        when(dParent.getSide()).thenReturn(SideType.SELL);
        assertEquals(parent.getRemainingQuantity(), ONE.negate());

        when(dParent.getOutstandingSize()).thenReturn(null);
        when(dParent.getSide()).thenReturn(SideType.BUY);
        assertNull(parent.getRemainingQuantity());

        when(dParent.getOutstandingSize()).thenReturn(ONE);
        when(dParent.getSide()).thenReturn(null);
        assertNull(parent.getRemainingQuantity());

    }

}
