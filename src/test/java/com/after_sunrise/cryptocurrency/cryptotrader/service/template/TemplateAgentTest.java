package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import org.mockito.InOrder;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class TemplateAgentTest {

    private TemplateAgent target;

    private Context context;

    @BeforeMethod
    public void setUp() throws Exception {

        context = mock(Context.class);

        target = spy(new TemplateAgent("test"));

    }

    @Test
    public void testGet() throws Exception {
        assertEquals(target.get(), "test");
    }

    @Test
    public void testManage() throws Exception {

        CreateInstruction i1 = CreateInstruction.builder().build();
        CreateInstruction i2 = CreateInstruction.builder().build();
        CreateInstruction i3 = CreateInstruction.builder().build();
        CancelInstruction i4 = CancelInstruction.builder().build();
        CancelInstruction i5 = CancelInstruction.builder().build();
        CancelInstruction i6 = CancelInstruction.builder().build();
        Instruction i7 = mock(Instruction.class);
        Instruction i8 = mock(Instruction.class);
        Request request = Request.builder().build();
        when(context.createOrder(any(), any())).thenReturn("create");
        when(context.cancelOrder(any(), any())).thenReturn("cancel");
        when(i7.accept(any())).thenReturn("mocked");
        when(i8.accept(any())).thenReturn("mocked");
        List<Instruction> values = asList(i1, i3, i5, i7, null, i2, i4, i6, i8);
        Map<Instruction, String> results = target.manage(context, request, values);
        assertEquals(results.size(), values.size() - 1);

        // Cancels are processed first. Unknowns are last.
        InOrder inOrder = inOrder(context);
        inOrder.verify(context).cancelOrder(any(), same(i5));
        inOrder.verify(context).cancelOrder(any(), same(i4));
        inOrder.verify(context).cancelOrder(any(), same(i6));
        inOrder.verify(context).createOrder(any(), same(i1));
        inOrder.verify(context).createOrder(any(), same(i3));
        inOrder.verify(context).createOrder(any(), same(i2));
        inOrder.verifyNoMoreInteractions();

        // Abort if invalid response.
        when(context.cancelOrder(any(), same(i5))).thenReturn("");
        results = target.manage(context, request, values);
        assertEquals(results.size(), 4);
        assertEquals(results.get(i4), "cancel");
        assertEquals(results.get(i6), "cancel");
        assertEquals(results.get(i7), "mocked");
        assertEquals(results.get(i8), "mocked");

        // No input
        assertEquals(target.manage(context, request, null).size(), 0);

    }

    @Test
    public void testReconcile() throws Exception {

        Map<Instruction, String> values = new IdentityHashMap<>();
        values.put(CreateInstruction.builder().build(), "i1");
        values.put(CancelInstruction.builder().build(), "i2");
        values.put(null, "i3");
        values.put(CancelInstruction.builder().build(), null);
        Request request = Request.builder().build();
        doReturn(TRUE).when(target).checkCreated(same(context), any(), eq("i1"), anyLong(), any());
        doReturn(TRUE).when(target).checkCancelled(same(context), any(), eq("i2"), anyLong(), any());

        Map<Instruction, Boolean> results = target.reconcile(context, request, values);
        assertEquals(results.size(), 2);
        results.values().forEach(Assert::assertTrue);
        verify(target).checkCreated(same(context), any(), eq("i1"), anyLong(), any());
        verify(target).checkCancelled(same(context), any(), eq("i2"), anyLong(), any());

        // No input
        assertEquals(target.reconcile(context, request, null).size(), 0);

    }

    @Test
    public void testCheckCreated() throws Exception {

        Key key = Key.from(null);
        String id = "id";
        Duration interval = Duration.ofMillis(1L);
        long retry = 10;

        // Not found
        when(context.findOrder(key, id)).thenReturn(null);
        assertFalse(target.checkCreated(context, key, id, retry, interval));

        // Interrupted
        Thread.currentThread().interrupt();
        assertFalse(target.checkCreated(context, key, id, retry, interval));

        // Found
        when(context.findOrder(key, id)).thenReturn(null, null, mock(Order.class));
        assertTrue(target.checkCreated(context, key, id, retry, interval));

    }

    @Test
    public void testCheckCancelled() throws Exception {

        Key key = Key.from(null);
        String id = "id";
        Duration interval = Duration.ofMillis(1L);
        long retry = 10;

        // Found but Active
        Order order = mock(Order.class);
        when(order.getActive()).thenReturn(TRUE);
        when(context.findOrder(key, id)).thenReturn(order);
        assertFalse(target.checkCancelled(context, key, id, retry, interval));

        // Interrupted
        Thread.currentThread().interrupt();
        assertFalse(target.checkCancelled(context, key, id, retry, interval));

        // Found and inactive
        when(order.getActive()).thenReturn(FALSE);
        when(context.findOrder(key, id)).thenReturn(order);
        assertTrue(target.checkCancelled(context, key, id, retry, interval));


        // Not found
        when(context.findOrder(key, id)).thenReturn(null);
        assertTrue(target.checkCancelled(context, key, id, retry, interval));

    }

}
