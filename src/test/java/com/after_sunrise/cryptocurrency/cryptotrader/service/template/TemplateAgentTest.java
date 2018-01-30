package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.google.common.collect.Sets;
import org.mockito.InOrder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

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

        CreateInstruction create1 = CreateInstruction.builder().build();
        CreateInstruction create2 = CreateInstruction.builder().build();
        CreateInstruction create3 = CreateInstruction.builder().build();
        CancelInstruction cancel1 = CancelInstruction.builder().build();
        CancelInstruction cancel2 = CancelInstruction.builder().build();
        CancelInstruction cancel3 = CancelInstruction.builder().build();

        Map<Instruction, String> values = new IdentityHashMap<>();
        values.put(create1, "i1");
        values.put(create2, "i2");
        values.put(create3, null);
        values.put(null, "i4");
        values.put(cancel1, "i5");
        values.put(cancel2, null);
        values.put(cancel3, "i7");

        Request request = Request.builder().site("s").instrument("i").currentTime(Instant.now()).build();
        doReturn(Key.from(request)).when(target).nextKey(any(), any());
        doReturn(null).when(context).getState(any());
        doReturn(null).when(context).findOrder(any(), anyString());

        // No orders
        Map<Instruction, Boolean> results = target.reconcile(context, request, values);
        assertEquals(results.size(), 4);
        assertEquals(results.get(create1), FALSE);
        assertEquals(results.get(create2), FALSE);
        assertEquals(results.get(cancel1), TRUE);
        assertEquals(results.get(cancel3), TRUE);
        verify(context, times(26)).findOrder(any(), anyString());
        verify(context, times(12)).findOrder(any(), eq("i1"));
        verify(context, times(12)).findOrder(any(), eq("i2"));
        verify(context, times(1)).findOrder(any(), eq("i5"));
        verify(context, times(1)).findOrder(any(), eq("i7"));

        // With orders
        Order o1 = mock(Order.class);
        Order o2 = mock(Order.class);
        when(o1.getActive()).thenReturn(null);
        when(o2.getActive()).thenReturn(TRUE);
        when(context.findOrder(any(), eq("i1"))).thenReturn(o1);
        when(context.findOrder(any(), eq("i2"))).thenReturn(o2);
        when(context.findOrder(any(), eq("i5"))).thenReturn(o1);
        when(context.findOrder(any(), eq("i7"))).thenReturn(o2);
        results = target.reconcile(context, request, values);
        assertEquals(results.size(), 4);
        assertEquals(results.get(create1), TRUE);
        assertEquals(results.get(create2), TRUE);
        assertEquals(results.get(cancel1), TRUE);
        assertEquals(results.get(cancel3), FALSE);
        verify(context, times(26 + 15)).findOrder(any(), anyString());
        verify(context, times(12 + 1)).findOrder(any(), eq("i1"));
        verify(context, times(12 + 1)).findOrder(any(), eq("i2"));
        verify(context, times(1 + 1)).findOrder(any(), eq("i5"));
        verify(context, times(1 + 12)).findOrder(any(), eq("i7"));

        // All success
        when(context.findOrder(any(), eq("i1"))).thenReturn(o1);
        when(context.findOrder(any(), eq("i2"))).thenReturn(o1);
        when(context.findOrder(any(), eq("i5"))).thenReturn(o1);
        when(context.findOrder(any(), eq("i7"))).thenReturn(o1);
        results = target.reconcile(context, request, values);
        assertEquals(results.size(), 4);
        assertEquals(results.get(create1), TRUE);
        assertEquals(results.get(create2), TRUE);
        assertEquals(results.get(cancel1), TRUE);
        assertEquals(results.get(cancel3), TRUE);
        verify(context, times(26 + 15 + 4)).findOrder(any(), anyString());
        verify(context, times(12 + 1 + 1)).findOrder(any(), eq("i1"));
        verify(context, times(12 + 1 + 1)).findOrder(any(), eq("i2"));
        verify(context, times(1 + 1 + 1)).findOrder(any(), eq("i5"));
        verify(context, times(1 + 12 + 1)).findOrder(any(), eq("i7"));

        // No input
        assertEquals(target.reconcile(context, request, null).size(), 0);

    }

    @Test
    public void testNextKey() {

        Key original = Key.builder().site("s").instrument("i").timestamp(Instant.now()).build();

        Key result = target.nextKey(original, Duration.ofMillis(100));
        assertEquals(result.getSite(), original.getSite());
        assertEquals(result.getInstrument(), original.getInstrument());
        assertEquals(result.getTimestamp(), original.getTimestamp().plusMillis(100));

        Thread.currentThread().interrupt();
        assertNull(target.nextKey(original, Duration.ofMillis(100)));

    }

}
