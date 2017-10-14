package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.core.ServiceFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Service;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.DOWN;
import static java.util.Collections.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class ContextImplTest {

    private ContextImpl target;

    private TestModule module;

    private Map<String, Context> contexts;

    private Key key;

    @BeforeMethod
    public void setUp() {

        key = Key.builder().site("c1").build();
        module = new TestModule();

        contexts = new LinkedHashMap<>();
        contexts.put("c1", mock(Context.class));
        contexts.put("c2", mock(Context.class));
        contexts.put("c3", mock(Context.class));
        contexts.put("c4", mock(Context.class));
        when(module.getMock(ServiceFactory.class).loadMap(Context.class)).thenReturn(contexts);

        target = new ContextImpl(module.createInjector());

    }

    @Test
    public void testClose() throws Exception {

        target.close();

        verify(contexts.get("c1")).close();
        verify(contexts.get("c2")).close();
        verify(contexts.get("c3")).close();
        verify(contexts.get("c4")).close();

    }

    @Test
    public void testClose_Exception() throws Exception {

        doThrow(new IOException("test1")).when(contexts.get("c2")).close();
        doThrow(new IOException("test2")).when(contexts.get("c3")).close();

        try {
            target.close();
        } catch (Exception e) {
            assertEquals(e.getSuppressed().length, 2);
        }

        verify(contexts.get("c1")).close();
        verify(contexts.get("c2")).close();
        verify(contexts.get("c3")).close();
        verify(contexts.get("c4")).close();

    }

    @Test
    public void testGet() throws Exception {

        assertEquals(target.get(), Service.WILDCARD);

    }

    @Test
    public void testForContext() throws Exception {

        // Found
        doReturn("hoge").when(contexts.get("c2")).get();
        assertEquals(target.forContext(Key.builder().site("c2").build(), Context::get), "hoge");

        // Not found
        doReturn("hoge").when(contexts.get("c2")).get();
        assertNull(target.forContext(Key.builder().build(), Context::get));

        // Null Key
        assertNull(target.forContext(null, Context::get));

    }

    @Test
    public void testGetBestAskPrice() {

        BigDecimal value = new BigDecimal(Math.random());

        when(contexts.get("c1").getBestAskPrice(key)).thenReturn(value);

        assertEquals(target.getBestAskPrice(key), value);

    }

    @Test
    public void testGetBestBidPrice() {

        BigDecimal value = new BigDecimal(Math.random());

        when(contexts.get("c1").getBestBidPrice(key)).thenReturn(value);

        assertEquals(target.getBestBidPrice(key), value);

    }

    @Test
    public void testGetMidPrice() {

        BigDecimal value = new BigDecimal(Math.random());

        when(contexts.get("c1").getMidPrice(key)).thenReturn(value);

        assertEquals(target.getMidPrice(key), value);

    }

    @Test
    public void testGetLastPrice() {

        BigDecimal value = new BigDecimal(Math.random());

        when(contexts.get("c1").getLastPrice(key)).thenReturn(value);

        assertEquals(target.getLastPrice(key), value);

    }

    @Test
    public void testListTrades() {

        Instant from = Instant.now();

        List<Trade> value = singletonList(mock(Trade.class));

        when(contexts.get("c1").listTrades(key, from)).thenReturn(value);

        assertEquals(target.listTrades(key, from), value);

    }

    @Test
    public void testGetInstrumentPosition() {

        BigDecimal value = new BigDecimal(Math.random());

        when(contexts.get("c1").getInstrumentPosition(key)).thenReturn(value);

        assertEquals(target.getInstrumentPosition(key), value);

    }

    @Test
    public void testGetFundingPosition() {

        BigDecimal value = new BigDecimal(Math.random());

        when(contexts.get("c1").getFundingPosition(key)).thenReturn(value);

        assertEquals(target.getFundingPosition(key), value);

    }

    @Test
    public void testRoundLotSize() {

        BigDecimal value = new BigDecimal(Math.random());

        when(contexts.get("c1").roundLotSize(key, ONE, DOWN)).thenReturn(value);

        assertEquals(target.roundLotSize(key, ONE, DOWN), value);

    }

    @Test
    public void testRoundTickSize() {

        BigDecimal value = new BigDecimal(Math.random());

        when(contexts.get("c1").roundTickSize(key, ONE, DOWN)).thenReturn(value);

        assertEquals(target.roundTickSize(key, ONE, DOWN), value);

    }

    @Test
    public void testGetCommissionRate() {

        BigDecimal value = new BigDecimal(Math.random());

        when(contexts.get("c1").getCommissionRate(key)).thenReturn(value);

        assertEquals(target.getCommissionRate(key), value);

    }

    @Test
    public void testIsMarginable() {

        Boolean value = false;

        when(contexts.get("c1").isMarginable(key)).thenReturn(value);

        assertEquals(target.isMarginable(key), value);

    }

    @Test
    public void testGetExpiry() {

        ZonedDateTime value = ZonedDateTime.now();

        when(contexts.get("c1").getExpiry(key)).thenReturn(value);

        assertEquals(target.getExpiry(key), value);

    }

    @Test
    public void testFindOrder() {

        String id = "foo";

        Order value = mock(Order.class);

        when(contexts.get("c1").findOrder(key, id)).thenReturn(value);

        assertEquals(target.findOrder(key, id), value);

    }

    @Test
    public void testListActiveOrders() {

        List<Order> value = singletonList(mock(Order.class));

        when(contexts.get("c1").listActiveOrders(key)).thenReturn(value);

        assertEquals(target.listActiveOrders(key), value);

    }

    @Test
    public void testListExecutions() {

        List<Order.Execution> value = singletonList(mock(Order.Execution.class));

        when(contexts.get("c1").listExecutions(key)).thenReturn(value);

        assertEquals(target.listExecutions(key), value);

    }

    @Test
    public void testCreateOrder() {

        CreateInstruction instruction = CreateInstruction.builder().build();

        String value = "testid";

        Map<CreateInstruction, String> results = singletonMap(instruction, value);

        when(contexts.get("c1").createOrders(key, singleton(instruction))).thenReturn(results);

        assertEquals(target.createOrders(key, singleton(instruction)), results);

    }

    @Test
    public void testCancelOrder() {

        CancelInstruction instruction = CancelInstruction.builder().build();

        String value = "testid";

        Map<CancelInstruction, String> results = singletonMap(instruction, value);

        when(contexts.get("c1").cancelOrders(key, singleton(instruction))).thenReturn(results);

        assertEquals(target.cancelOrders(key, singleton(instruction)), results);

    }

}
