package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class TemplateContextTest {

    private static class TestContext extends TemplateContext {

        private TestContext() {
            super("test");
        }

        @Override
        public void close() throws Exception {
        }

        @Override
        public BigDecimal getBestAskPrice(Key key) {
            return null;
        }

        @Override
        public BigDecimal getBestBidPrice(Key key) {
            return null;
        }

        @Override
        public BigDecimal getMidPrice(Key key) {
            return null;
        }

        @Override
        public BigDecimal getLastPrice(Key key) {
            return null;
        }

        @Override
        public BigDecimal getInstrumentPosition(Key key) {
            return null;
        }

        @Override
        public BigDecimal getFundingPosition(Key key) {
            return null;
        }

        @Override
        public BigDecimal roundLotSize(Key key, BigDecimal value, RoundingMode mode) {
            return null;
        }

        @Override
        public BigDecimal roundTickSize(Key key, BigDecimal value, RoundingMode mode) {
            return null;
        }

        @Override
        public BigDecimal getCommissionRate(Key key) {
            return null;
        }

        @Override
        public Boolean isMarginable(Key key) {
            return null;
        }

        @Override
        public ZonedDateTime getExpiry(Key key) {
            return null;
        }

        @Override
        public Order findOrder(Key key, String id) {
            return null;
        }

        @Override
        public List<Order> listActiveOrders(Key key) {
            return null;
        }

        @Override
        public List<Order.Execution> listExecutions(Key key) {
            return null;
        }

        @Override
        public String createOrder(Key key, Instruction.CreateInstruction instruction) {
            return null;
        }

        @Override
        public String cancelOrder(Key key, Instruction.CancelInstruction instruction) {
            return null;
        }

        @Override
        public List<Trade> listTrades(Key key, Instant fromTime) {
            return null;
        }

    }

    private TemplateContext target;

    @BeforeMethod
    public void setUp() {
        target = new TestContext();
    }

    @Test
    public void testGet() throws Exception {
        assertEquals(target.get(), "test");
    }

    @Test
    public void testFindCached() throws Exception {

        Key key = Key.from(null);
        Callable<BigDecimal> callable = mock(Callable.class);
        when(callable.call()).thenReturn(ONE, TEN, null);

        assertEquals(target.findCached(BigDecimal.class, key, callable), ONE);
        assertEquals(target.findCached(BigDecimal.class, key, callable), ONE);
        verify(callable).call();

        target.clear();
        assertEquals(target.findCached(BigDecimal.class, key, callable), TEN);
        assertEquals(target.findCached(BigDecimal.class, key, callable), TEN);
        verify(callable, times(2)).call();

        target.clear();
        assertNull(target.findCached(BigDecimal.class, key, callable));
        assertNull(target.findCached(BigDecimal.class, key, callable));
        verify(callable, times(3)).call();

        target.clear();
        doThrow(new Exception("test")).when(callable).call();
        assertNull(target.findCached(BigDecimal.class, key, callable));
        assertNull(target.findCached(BigDecimal.class, key, callable));
        verify(callable, times(5)).call();

        target.clear();
        assertNull(target.findCached(null, key, callable));
        assertNull(target.findCached(BigDecimal.class, null, callable));
        verifyNoMoreInteractions(callable);

    }

    @Test
    public void testListCached() throws Exception {

        Key key = Key.from(null);
        Callable<List<BigDecimal>> callable = mock(Callable.class);
        when(callable.call()).thenReturn(singletonList(ONE), singletonList(TEN), null);

        assertEquals(target.listCached(BigDecimal.class, key, callable), singletonList(ONE));
        assertEquals(target.listCached(BigDecimal.class, key, callable), singletonList(ONE));
        verify(callable).call();

        target.clear();
        assertEquals(target.listCached(BigDecimal.class, key, callable), singletonList(TEN));
        assertEquals(target.listCached(BigDecimal.class, key, callable), singletonList(TEN));
        verify(callable, times(2)).call();

        target.clear();
        assertEquals(target.listCached(BigDecimal.class, key, callable), emptyList());
        assertEquals(target.listCached(BigDecimal.class, key, callable), emptyList());
        verify(callable, times(3)).call();

        target.clear();
        doThrow(new Exception("test")).when(callable).call();
        assertEquals(target.listCached(BigDecimal.class, key, callable), emptyList());
        assertEquals(target.listCached(BigDecimal.class, key, callable), emptyList());
        verify(callable, times(5)).call();

        target.clear();
        assertEquals(target.listCached(null, key, callable), emptyList());
        assertEquals(target.listCached(BigDecimal.class, null, callable), emptyList());
        verifyNoMoreInteractions(callable);

    }

    @Test
    public void testGetQuietly() throws Exception {

        CompletableFuture<BigDecimal> future = null;
        Duration timeout = null;
        assertNull(target.getQuietly(future, timeout));

        future = CompletableFuture.completedFuture(ONE);
        assertEquals(target.getQuietly(future, timeout), ONE);

        timeout = Duration.ofMillis(1);
        assertEquals(target.getQuietly(future, timeout), ONE);

        future = null;
        timeout = Duration.ofMillis(1);
        assertNull(target.getQuietly(future, timeout));

        future = new CompletableFuture<>();
        future.completeExceptionally(new Exception("test"));
        assertNull(target.getQuietly(future, timeout));

        timeout = null;
        assertNull(target.getQuietly(future, timeout));

    }

}
