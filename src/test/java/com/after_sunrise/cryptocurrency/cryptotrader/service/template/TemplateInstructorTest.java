package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Adviser.Advice;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import org.mockito.invocation.InvocationOnMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.*;
import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class TemplateInstructorTest {

    private TemplateInstructor target;

    private Context context;

    private Request.RequestBuilder builder;

    @BeforeMethod
    public void setUp() throws Exception {

        context = mock(Context.class);

        BiFunction<InvocationOnMock, BigDecimal, BigDecimal> f = (i, unit) -> {

            RoundingMode mode = i.getArgumentAt(2, RoundingMode.class);

            BigDecimal value = i.getArgumentAt(1, BigDecimal.class);

            if (value == null || mode == null) {
                return null;
            }

            BigDecimal units = value.divide(unit, INTEGER_ZERO, mode);

            return units.multiply(unit);

        };
        when(context.roundTickSize(any(), any(), any())).thenAnswer(i -> f.apply(i, new BigDecimal("0.003")));
        when(context.roundLotSize(any(), any(), any())).thenAnswer(i -> f.apply(i, new BigDecimal("0.3")));

        builder = Request.builder().site("s").instrument("i").timestamp(now())
                .tradingExposure(ZERO).tradingSplit(new BigDecimal("5")).tradingSpread(ZERO);

        target = spy(new TemplateInstructor("test"));

    }

    @Test
    public void testGet() throws Exception {
        Assert.assertEquals(target.get(), "test");
    }

    @Test
    public void testInstruct() throws Exception {

        Request request = builder.build();
        Advice advice = Advice.builder().build();
        CreateInstruction i1 = CreateInstruction.builder().build();
        CreateInstruction i2 = CreateInstruction.builder().build();
        CancelInstruction i3 = CancelInstruction.builder().build();
        Map<CancelInstruction, Order> cancels = singletonMap(i3, mock(Order.class));
        List<Instruction> instructions = asList(i3, i1, i2);

        doReturn(singletonList(i1)).when(target).createBuys(context, request, advice);
        doReturn(singletonList(i2)).when(target).createSells(context, request, advice);
        doReturn(cancels).when(target).createCancels(context, request);
        doReturn(instructions).when(target).merge(asList(i1, i2), cancels);

        assertSame(target.instruct(context, request, advice), instructions);

        // Null inputs
        assertEquals(target.instruct(context, null, advice).size(), 0);
        assertEquals(target.instruct(context, request, null).size(), 0);

    }

    @Test
    public void testCreateCancels() throws Exception {

        Order o1 = mock(Order.class);
        Order o2 = mock(Order.class);
        Order o3 = mock(Order.class);
        Order o4 = mock(Order.class);
        when(o1.getActive()).thenReturn(null);
        when(o2.getActive()).thenReturn(TRUE);
        when(o3.getActive()).thenReturn(TRUE);
        when(o4.getActive()).thenReturn(FALSE);
        when(o2.getId()).thenReturn("id");
        List<Order> orders = asList(o1, null, o2, o3, o4);

        when(context.listOrders(Key.from(builder.build()))).thenReturn(orders);
        Map<CancelInstruction, Order> results = target.createCancels(context, builder.build());
        assertEquals(results.size(), 1);
        assertTrue(results.values().contains(o2));

        when(context.listOrders(Key.from(builder.build()))).thenReturn(null);
        results = target.createCancels(context, builder.build());
        assertEquals(results.size(), 0);

    }

    @Test
    public void testCreateBuys() throws Exception {

        Request request = builder.build();
        Advice advice = Advice.builder()
                .buyLimitPrice(ONE).buyLimitSize(TEN)
                .build();

        List<CreateInstruction> results = target.createBuys(context, request, advice);
        assertEquals(results.size(), 5, results.toString());

        assertEquals(results.get(0).getPrice(), new BigDecimal("0.999"));
        assertEquals(results.get(1).getPrice(), new BigDecimal("0.996"));
        assertEquals(results.get(2).getPrice(), new BigDecimal("0.993"));
        assertEquals(results.get(3).getPrice(), new BigDecimal("0.990"));
        assertEquals(results.get(4).getPrice(), new BigDecimal("0.987"));

        assertEquals(results.get(0).getSize(), new BigDecimal("2.1"));
        assertEquals(results.get(1).getSize(), new BigDecimal("2.1"));
        assertEquals(results.get(2).getSize(), new BigDecimal("2.1"));
        assertEquals(results.get(3).getSize(), new BigDecimal("1.8"));
        assertEquals(results.get(4).getSize(), new BigDecimal("1.8"));

        // Null size
        results = target.createBuys(context, request, Advice.builder().build());
        assertEquals(results.size(), 0);

        // Zero size
        results = target.createBuys(context, request, Advice.builder().sellLimitSize(ONE.negate()).build());
        assertEquals(results.size(), 0);

        // Negative size
        results = target.createBuys(context, request, Advice.builder().sellLimitSize(ONE.negate()).build());
        assertEquals(results.size(), 0);

        // Too small
        results = target.createBuys(context, request, Advice.builder().sellLimitSize(ONE.movePointLeft(10)).build());
        assertEquals(results.size(), 0);

    }

    @Test
    public void testCreateSells() throws Exception {

        Request request = builder.build();
        Advice advice = Advice.builder()
                .sellLimitPrice(ONE).sellLimitSize(TEN)
                .build();

        List<CreateInstruction> results = target.createSells(context, request, advice);
        assertEquals(results.size(), 5, results.toString());

        assertEquals(results.get(0).getPrice(), new BigDecimal("1.002"));
        assertEquals(results.get(1).getPrice(), new BigDecimal("1.005"));
        assertEquals(results.get(2).getPrice(), new BigDecimal("1.008"));
        assertEquals(results.get(3).getPrice(), new BigDecimal("1.011"));
        assertEquals(results.get(4).getPrice(), new BigDecimal("1.014"));

        assertEquals(results.get(0).getSize(), new BigDecimal("-2.1"));
        assertEquals(results.get(1).getSize(), new BigDecimal("-2.1"));
        assertEquals(results.get(2).getSize(), new BigDecimal("-2.1"));
        assertEquals(results.get(3).getSize(), new BigDecimal("-1.8"));
        assertEquals(results.get(4).getSize(), new BigDecimal("-1.8"));

        // Null size
        results = target.createSells(context, request, Advice.builder().build());
        assertEquals(results.size(), 0);

        // Zero size
        results = target.createSells(context, request, Advice.builder().sellLimitSize(ONE.negate()).build());
        assertEquals(results.size(), 0);

        // Negative size
        results = target.createSells(context, request, Advice.builder().sellLimitSize(ONE.negate()).build());
        assertEquals(results.size(), 0);

        // Too small
        results = target.createSells(context, request, Advice.builder().sellLimitSize(ONE.movePointLeft(10)).build());
        assertEquals(results.size(), 0);

    }

    @Test
    public void testMerge() throws Exception {

        CreateInstruction create1 = CreateInstruction.builder().price(valueOf(11)).size(valueOf(21)).build();
        CreateInstruction create2 = CreateInstruction.builder().price(valueOf(12)).size(valueOf(22)).build();
        CreateInstruction create3 = CreateInstruction.builder().price(valueOf(13)).size(valueOf(23)).build();
        CreateInstruction create4 = CreateInstruction.builder().price(valueOf(14)).size(valueOf(24)).build();
        CreateInstruction create5 = CreateInstruction.builder().price(valueOf(15)).size(valueOf(25)).build();
        List<CreateInstruction> creates = asList(create4, create2, create3, create5, create1); // Shuffled

        CancelInstruction cancel1 = CancelInstruction.builder().build();
        CancelInstruction cancel2 = CancelInstruction.builder().build();
        CancelInstruction cancel3 = CancelInstruction.builder().build();
        CancelInstruction cancel4 = CancelInstruction.builder().build();
        Map<CancelInstruction, Order> cancels = new IdentityHashMap<>();
        cancels.put(cancel1, mock(Order.class)); // Net
        cancels.put(cancel2, mock(Order.class)); // Cancel - no price
        cancels.put(cancel3, mock(Order.class)); // Cancel - no size
        cancels.put(cancel4, mock(Order.class)); // Net
        when(cancels.get(cancel1).getOrderPrice()).thenReturn(create1.getPrice());
        when(cancels.get(cancel1).getRemainingQuantity()).thenReturn(create1.getSize());
        when(cancels.get(cancel2).getOrderPrice()).thenReturn(null);
        when(cancels.get(cancel2).getRemainingQuantity()).thenReturn(create2.getSize());
        when(cancels.get(cancel3).getOrderPrice()).thenReturn(create3.getPrice());
        when(cancels.get(cancel3).getRemainingQuantity()).thenReturn(null);
        when(cancels.get(cancel4).getOrderPrice()).thenReturn(create4.getPrice());
        when(cancels.get(cancel4).getRemainingQuantity()).thenReturn(create4.getSize());

        List<Instruction> results = target.merge(creates, cancels);
        assertEquals(results.size(), 5); // cancel(o2, o3) + create(c2, c4, c5)
        assertTrue(results.contains(cancel2));
        assertTrue(results.contains(cancel3));
        assertTrue(results.contains(create2));
        assertTrue(results.contains(create3));
        assertTrue(results.contains(create5));

    }

}