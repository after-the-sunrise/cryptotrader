package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
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
        CreateInstruction i2 = null;
        CancelInstruction i3 = CancelInstruction.builder().build();
        List<Instruction> values = Arrays.asList(i1, i2, i3);
        Request request = Request.builder().build();
        when(context.createOrder(any(), any())).thenReturn("create");
        when(context.cancelOrder(any(), any())).thenReturn("cancel");

        Map<Instruction, String> results = target.manage(context, request, values);
        assertEquals(results.size(), 2);
        assertEquals(results.get(i1), "create");
        assertEquals(results.get(i3), "cancel");
        verify(context).createOrder(any(), same(i1));
        verify(context).cancelOrder(any(), same(i3));

        // No input
        assertEquals(target.manage(context, request, null).size(), 0);
        verifyNoMoreInteractions(context);

    }

    @Test
    public void testReconcile() throws Exception {

        Map<Instruction, String> values = new IdentityHashMap<>();
        values.put(CreateInstruction.builder().build(), "i1");
        values.put(CancelInstruction.builder().build(), "i2");
        values.put(null, "i3");
        values.put(CancelInstruction.builder().build(), null);
        Request request = Request.builder().build();
        doReturn(TRUE).when(target).checkCreated(same(context), any(), eq("i1"), any());
        doReturn(TRUE).when(target).checkCancelled(same(context), any(), eq("i2"), any());

        Map<Instruction, Boolean> results = target.reconcile(context, request, values);
        assertEquals(results.size(), 2);
        results.values().forEach(Assert::assertTrue);
        verify(target).checkCreated(same(context), any(), eq("i1"), any());
        verify(target).checkCancelled(same(context), any(), eq("i2"), any());

        // No input
        assertEquals(target.reconcile(context, request, null).size(), 0);

    }

    @Test
    public void testCheckCreated() throws Exception {

        Key key = Key.from(null);
        String id = "id";
        Duration interval = Duration.ofMillis(1L);

        // Not found
        when(context.findOrder(key, id)).thenReturn(null);
        assertFalse(target.checkCreated(context, key, id, interval));

        // Interrupted
        Thread.currentThread().interrupt();
        assertFalse(target.checkCreated(context, key, id, interval));

        // Found
        when(context.findOrder(key, id)).thenReturn(null, null, mock(Order.class));
        assertTrue(target.checkCreated(context, key, id, interval));

    }

    @Test
    public void testCheckCancelled() throws Exception {

        Key key = Key.from(null);
        String id = "id";
        Duration interval = Duration.ofMillis(1L);

        // Found but Active
        Order order = mock(Order.class);
        when(order.getActive()).thenReturn(TRUE);
        when(context.findOrder(key, id)).thenReturn(order);
        assertFalse(target.checkCancelled(context, key, id, interval));

        // Interrupted
        Thread.currentThread().interrupt();
        assertFalse(target.checkCancelled(context, key, id, interval));

        // Found and inactive
        when(order.getActive()).thenReturn(FALSE);
        when(context.findOrder(key, id)).thenReturn(order);
        assertTrue(target.checkCancelled(context, key, id, interval));


        // Not found
        when(context.findOrder(key, id)).thenReturn(null);
        assertTrue(target.checkCancelled(context, key, id, interval));

    }

}
