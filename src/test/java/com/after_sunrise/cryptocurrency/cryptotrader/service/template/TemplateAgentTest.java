package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.StateType;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.google.common.collect.Sets;
import org.mockito.InOrder;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
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
        CancelInstruction i4 = CancelInstruction.builder().id("i4").build();
        CancelInstruction i5 = CancelInstruction.builder().id("i5").build();
        CancelInstruction i6 = CancelInstruction.builder().id("i6").build();
        Instruction i7 = mock(Instruction.class);
        Instruction i8 = mock(Instruction.class);
        Request request = Request.builder().build();

        when(context.createOrders(any(), any())).thenAnswer(invocation -> {
            Map<CreateInstruction, String> results = new IdentityHashMap<>();
            Set<?> instructions = invocation.getArgumentAt(1, Set.class);
            instructions.stream().map(CreateInstruction.class::cast).forEach(i -> results.put(i, i.getUid()));
            return results;
        });

        when(context.cancelOrders(any(), any())).thenAnswer(invocation -> {
            Map<CancelInstruction, String> results = new IdentityHashMap<>();
            Set<?> instructions = invocation.getArgumentAt(1, Set.class);
            instructions.stream().map(CancelInstruction.class::cast).forEach(i -> results.put(i, i.getId()));
            return results;
        });

        // Invoke
        List<Instruction> values = asList(i1, i3, i5, i7, null, i2, i4, i6, i8);
        Map<Instruction, String> results = target.manage(context, request, values);
        assertEquals(results.size(), 3 + 3); // Mocks are ignored.

        // Cancels are processed first. Unknowns are last.
        InOrder inOrder = inOrder(context);
        inOrder.verify(context).cancelOrders(any(), eq(Sets.newHashSet(i4, i5, i6)));
        inOrder.verify(context).createOrders(any(), eq(Sets.newHashSet(i1, i2, i3)));
        inOrder.verifyNoMoreInteractions();

        // Abort if invalid response.
        doReturn(singletonMap(i4, null)).when(context).cancelOrders(any(), any());
        results = target.manage(context, request, values);
        assertEquals(results.size(), 1);
        assertEquals(results.get(i4), null);

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
        doReturn(TRUE).when(target).checkCreated(same(context), any(), eq("i1"), any(), any());
        doReturn(TRUE).when(target).checkCancelled(same(context), any(), eq("i2"), any(), any());

        Map<Instruction, Boolean> results = target.reconcile(context, request, values);
        assertEquals(results.size(), 2);
        results.values().forEach(Assert::assertTrue);
        verify(target).checkCreated(same(context), any(), eq("i1"), any(), any());
        verify(target).checkCancelled(same(context), any(), eq("i2"), any(), any());

        // No input
        assertEquals(target.reconcile(context, request, null).size(), 0);

    }

    @Test
    public void testCheckCreated() throws Exception {

        Key key = Key.builder().timestamp(Instant.ofEpochMilli(1000)).build();
        String id = "id";
        Duration interval = Duration.ofMillis(1L);

        // Not found
        when(context.findOrder(any(), eq(id))).thenReturn(null);
        assertFalse(target.checkCreated(context, key, id, new AtomicLong(10), interval));

        // Interrupted
        Thread.currentThread().interrupt();
        assertFalse(target.checkCreated(context, key, id, new AtomicLong(10), interval));

        // Terminated
        when(context.getState(any())).thenReturn(StateType.TERMINATE, null, null);
        assertFalse(target.checkCreated(context, key, id, new AtomicLong(10), interval));

        // Found
        when(context.findOrder(any(), eq(id))).thenReturn(null, null, mock(Order.class), null);
        assertTrue(target.checkCreated(context, key, id, new AtomicLong(10), interval));

    }

    @Test
    public void testCheckCancelled() throws Exception {

        Key key = Key.builder().timestamp(Instant.ofEpochMilli(1000)).build();
        String id = "id";
        Duration interval = Duration.ofMillis(1L);

        // Found but Active
        Order order = mock(Order.class);
        when(order.getActive()).thenReturn(TRUE);
        when(context.findOrder(any(), eq(id))).thenReturn(order);
        assertFalse(target.checkCancelled(context, key, id, new AtomicLong(10), interval));

        // Interrupted
        Thread.currentThread().interrupt();
        assertFalse(target.checkCancelled(context, key, id, new AtomicLong(10), interval));

        // Terminated
        when(context.getState(any())).thenReturn(StateType.TERMINATE, null, null);
        assertFalse(target.checkCancelled(context, key, id, new AtomicLong(10), interval));

        // Found and inactive
        when(order.getActive()).thenReturn(FALSE);
        when(context.findOrder(any(), eq(id))).thenReturn(order);
        assertTrue(target.checkCancelled(context, key, id, new AtomicLong(10), interval));

        // Not found
        when(context.findOrder(any(), eq(id))).thenReturn(null);
        assertTrue(target.checkCancelled(context, key, id, new AtomicLong(10), interval));

    }

}
