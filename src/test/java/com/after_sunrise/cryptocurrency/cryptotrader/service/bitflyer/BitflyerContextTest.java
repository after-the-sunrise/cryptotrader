package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.Bitflyer4j;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.*;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.AccountService;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.MarketService;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.OrderService;
import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ProductType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static com.after_sunrise.cryptocurrency.bitflyer4j.core.ConditionType.LIMIT;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.ConditionType.MARKET;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType.BUY;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType.SELL;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.DOWN;
import static java.math.RoundingMode.UP;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitflyerContextTest {

    private BitflyerContext target;

    private TestModule module;

    private AccountService accountService;

    private MarketService marketService;

    private OrderService orderService;

    @BeforeMethod
    public void setUp() {

        module = new TestModule();
        accountService = module.getMock(AccountService.class);
        marketService = module.getMock(MarketService.class);
        orderService = module.getMock(OrderService.class);

        when(module.getMock(Bitflyer4j.class).getAccountService()).thenReturn(accountService);
        when(module.getMock(Bitflyer4j.class).getMarketService()).thenReturn(marketService);
        when(module.getMock(Bitflyer4j.class).getOrderService()).thenReturn(orderService);

        target = spy(new BitflyerContext());
        target.initialize(module.createInjector());

    }

    @Test
    public void testGetBesAskPrice() throws Exception {

        Key key = Key.from(Request.builder().instrument("i").build());

        CompletableFuture<Tick> f1 = completedFuture(mock(Tick.class));
        CompletableFuture<Tick> f2 = completedFuture(mock(Tick.class));
        when(f1.get().getBestAskPrice()).thenReturn(ONE);
        when(f2.get().getBestAskPrice()).thenReturn(TEN);
        when(marketService.getTick("i")).thenReturn(f1, f2);

        assertEquals(target.getBestAskPrice(key), ONE);
        assertEquals(target.getBestAskPrice(key), ONE);
        target.clear();
        assertEquals(target.getBestAskPrice(key), TEN);
        assertEquals(target.getBestAskPrice(key), TEN);

    }

    @Test
    public void testGetBesBidPrice() throws Exception {

        Key key = Key.from(Request.builder().instrument("i").build());

        CompletableFuture<Tick> f1 = completedFuture(mock(Tick.class));
        CompletableFuture<Tick> f2 = completedFuture(mock(Tick.class));
        when(f1.get().getBestBidPrice()).thenReturn(ONE);
        when(f2.get().getBestBidPrice()).thenReturn(TEN);
        when(marketService.getTick("i")).thenReturn(f1, f2);

        assertEquals(target.getBestBidPrice(key), ONE);
        assertEquals(target.getBestBidPrice(key), ONE);
        target.clear();
        assertEquals(target.getBestBidPrice(key), TEN);
        assertEquals(target.getBestBidPrice(key), TEN);

    }

    @Test
    public void testGetMidPrice() throws Exception {

        Key key = Key.from(Request.builder().instrument("i").build());

        CompletableFuture<Board> f1 = completedFuture(mock(Board.class));
        CompletableFuture<Board> f2 = completedFuture(mock(Board.class));
        when(f1.get().getMid()).thenReturn(ONE);
        when(f2.get().getMid()).thenReturn(TEN);
        when(marketService.getBoard("i")).thenReturn(f1, f2);

        assertEquals(target.getMidPrice(key), ONE);
        assertEquals(target.getMidPrice(key), ONE);
        target.clear();
        assertEquals(target.getMidPrice(key), TEN);
        assertEquals(target.getMidPrice(key), TEN);

    }

    @Test
    public void testGetLastPrice() throws Exception {

        Key key = Key.from(Request.builder().instrument("i").build());

        CompletableFuture<Tick> f1 = completedFuture(mock(Tick.class));
        CompletableFuture<Tick> f2 = completedFuture(mock(Tick.class));
        when(f1.get().getTradePrice()).thenReturn(ONE);
        when(f2.get().getTradePrice()).thenReturn(TEN);
        when(marketService.getTick("i")).thenReturn(f1, f2);

        assertEquals(target.getLastPrice(key), ONE);
        assertEquals(target.getLastPrice(key), ONE);
        target.clear();
        assertEquals(target.getLastPrice(key), TEN);
        assertEquals(target.getLastPrice(key), TEN);

    }

    @Test
    public void testForBalance() throws Exception {


        Balance b1 = null;
        Balance b2 = mock(Balance.class);
        Balance b3 = mock(Balance.class);

        when(b2.getCurrency()).thenReturn("BTC");
        when(b2.getAmount()).thenReturn(TEN);

        CompletableFuture<List<Balance>> f1 = completedFuture(asList(b1, null, b2));
        CompletableFuture<List<Balance>> f2 = completedFuture(asList(b2, null, b3));
        when(accountService.getBalances()).thenReturn(f1, f2, null);


        // Null key
        Key key = null;
        assertNull(target.forBalance(key, ProductType::getStructure, Balance::getAmount));
        verifyNoMoreInteractions(accountService);

        // No instrument
        key = Key.from(Request.builder().build());
        assertNull(target.forBalance(key, ProductType::getStructure, Balance::getAmount));
        verifyNoMoreInteractions(accountService);

        // Unknown instrument
        key = Key.from(Request.builder().instrument("hoge").build());
        assertNull(target.forBalance(key, ProductType::getStructure, Balance::getAmount));
        verifyNoMoreInteractions(accountService);

        // Found
        key = Key.from(Request.builder().instrument("BTC_JPY").build());
        assertEquals(target.forBalance(key, ProductType::getStructure, Balance::getAmount), TEN);
        assertEquals(target.forBalance(key, ProductType::getStructure, Balance::getAmount), TEN);

        // Next query
        target.clear();
        assertEquals(target.forBalance(key, ProductType::getStructure, Balance::getAmount), TEN);
        assertEquals(target.forBalance(key, ProductType::getStructure, Balance::getAmount), TEN);

        // Null result
        target.clear();
        assertNull(target.forBalance(key, ProductType::getStructure, Balance::getAmount));
        assertNull(target.forBalance(key, ProductType::getStructure, Balance::getAmount));

    }

    @Test
    public void testGetInstrumentPosition() throws Exception {

        Key key = Key.from(Request.builder().instrument("BTC_JPY").build());

        CompletableFuture<List<Balance>> f1 = completedFuture(singletonList(mock(Balance.class)));
        CompletableFuture<List<Balance>> f2 = completedFuture(singletonList(mock(Balance.class)));
        when(f1.get().get(0).getCurrency()).thenReturn("BTC");
        when(f2.get().get(0).getCurrency()).thenReturn("BTC");
        when(f1.get().get(0).getAmount()).thenReturn(ONE);
        when(f2.get().get(0).getAmount()).thenReturn(TEN);
        when(accountService.getBalances()).thenReturn(f1, f2);

        assertEquals(target.getInstrumentPosition(key), ONE);
        assertEquals(target.getInstrumentPosition(key), ONE);
        target.clear();
        assertEquals(target.getInstrumentPosition(key), TEN);
        assertEquals(target.getInstrumentPosition(key), TEN);

    }

    @Test
    public void testGetFundingPosition() throws Exception {

        Key key = Key.from(Request.builder().instrument("BTC_JPY").build());

        CompletableFuture<List<Balance>> f1 = completedFuture(singletonList(mock(Balance.class)));
        CompletableFuture<List<Balance>> f2 = completedFuture(singletonList(mock(Balance.class)));
        when(f1.get().get(0).getCurrency()).thenReturn("JPY");
        when(f2.get().get(0).getCurrency()).thenReturn("JPY");
        when(f1.get().get(0).getAmount()).thenReturn(ONE);
        when(f2.get().get(0).getAmount()).thenReturn(TEN);
        when(accountService.getBalances()).thenReturn(f1, f2);

        assertEquals(target.getFundingPosition(key), ONE);
        assertEquals(target.getFundingPosition(key), ONE);
        target.clear();
        assertEquals(target.getFundingPosition(key), TEN);
        assertEquals(target.getFundingPosition(key), TEN);

    }

    @Test
    public void testRoundLotSize() throws Exception {

        Key key = Key.from(Request.builder().instrument("BTC_JPY").build());
        BigDecimal input = new BigDecimal("0.1234");

        assertEquals(target.roundLotSize(key, input, DOWN), new BigDecimal("0.123"));
        assertEquals(target.roundLotSize(key, input, UP), new BigDecimal("0.124"));
        assertNull(target.roundLotSize(null, input, UP));
        assertNull(target.roundLotSize(key, null, UP));
        assertNull(target.roundLotSize(key, input, null));
        assertNull(target.roundLotSize(Key.from(Request.builder().instrument("TEST").build()), input, UP));

    }

    @Test
    public void testRoundTickSize() throws Exception {

        Key key = Key.from(Request.builder().instrument("BTC_JPY").build());
        BigDecimal input = new BigDecimal("123.45");

        assertEquals(target.roundTickSize(key, input, DOWN), new BigDecimal("123"));
        assertEquals(target.roundTickSize(key, input, UP), new BigDecimal("124"));
        assertNull(target.roundTickSize(null, input, UP));
        assertNull(target.roundTickSize(key, null, UP));
        assertNull(target.roundTickSize(key, input, null));
        assertNull(target.roundTickSize(Key.from(Request.builder().instrument("TEST").build()), input, UP));

    }

    @Test
    public void testFetchOrder() {

        Key key = Key.from(Request.builder().instrument("inst").build());

        OrderList.Response r1 = mock(OrderList.Response.class);
        OrderList.Response r2 = mock(OrderList.Response.class);
        when(r1.getPrice()).thenReturn(ONE);
        when(r2.getPrice()).thenReturn(TEN);
        when(orderService.listOrders(any(), any())).thenAnswer(i -> {

            assertEquals(i.getArgumentAt(0, OrderList.class).getProduct(), "inst");
            assertNull(i.getArgumentAt(0, OrderList.class).getState());
            assertNull(i.getArgumentAt(0, OrderList.class).getAcceptanceId());
            assertNull(i.getArgumentAt(0, OrderList.class).getOrderId());
            assertNull(i.getArgumentAt(0, OrderList.class).getParentId());
            assertNull(i.getArgumentAt(1, Pagination.class));

            return completedFuture(asList(r1, null, r2));

        }).thenReturn(null);

        // Queried
        List<Order> orders = target.fetchOrder(key);
        assertEquals(orders.size(), 2);
        assertEquals(orders.get(0).getOrderPrice(), ONE);
        assertEquals(orders.get(1).getOrderPrice(), TEN);
        verify(orderService).listOrders(any(), any());

        // Cached
        orders = target.fetchOrder(key);
        assertEquals(orders.size(), 2);
        assertEquals(orders.get(0).getOrderPrice(), ONE);
        assertEquals(orders.get(1).getOrderPrice(), TEN);
        verify(orderService).listOrders(any(), any());

        target.clear();

        // Queried
        orders = target.fetchOrder(key);
        assertEquals(orders.size(), 0);
        verify(orderService, times(2)).listOrders(any(), any());

    }

    @Test
    public void testFindOrder() throws Exception {

        Key key = Key.from(Request.builder().instrument("inst").build());

        Order o1 = mock(Order.class);
        Order o2 = mock(Order.class);
        Order o3 = mock(Order.class);
        when(o2.getId()).thenReturn("id");
        doReturn(asList(o1, o2, o3)).when(target).fetchOrder(key);

        assertSame(target.findOrder(key, "id"), o2);
        assertNull(target.findOrder(key, "di"));
        assertNull(target.findOrder(key, null));

        reset(o2);

        assertNull(target.findOrder(key, "id"));
        assertNull(target.findOrder(key, "di"));
        assertNull(target.findOrder(key, null));

    }

    @Test
    public void testListOrders() throws Exception {

        Key key = Key.from(Request.builder().instrument("inst").build());

        Order o1 = mock(Order.class);
        Order o2 = mock(Order.class);
        Order o3 = mock(Order.class);
        Order o4 = mock(Order.class);
        when(o2.getActive()).thenReturn(true);
        when(o3.getActive()).thenReturn(false);
        when(o4.getActive()).thenReturn(true);
        doReturn(asList(o1, o2, o3, o4)).when(target).fetchOrder(key);

        List<Order> results = target.listOrders(key);
        assertEquals(results.size(), 2);
        assertSame(results.get(0), o2);
        assertSame(results.get(1), o4);

    }

    @Test
    public void testCreateOrder() throws Exception {

        Key key = Key.from(Request.builder().instrument("inst").build());
        CreateInstruction.CreateInstructionBuilder builder = CreateInstruction.builder().price(TEN).size(ONE);
        CompletableFuture<OrderCreate.Response> future = completedFuture(mock(OrderCreate.Response.class));
        AtomicReference<OrderCreate> reference = new AtomicReference<>();

        when(future.get().getAcceptanceId()).thenReturn("aid");
        when(orderService.sendOrder(any())).thenAnswer(i -> {

            reference.set(i.getArgumentAt(0, OrderCreate.class));

            return future;

        });

        // Buy
        assertEquals(target.createOrder(key, builder.build()), future.get().getAcceptanceId());
        verify(orderService, times(1)).sendOrder(any());
        assertEquals(reference.get().getProduct(), key.getInstrument());
        assertEquals(reference.get().getType(), LIMIT);
        assertEquals(reference.get().getSide(), BUY);
        assertEquals(reference.get().getPrice(), TEN);
        assertEquals(reference.get().getSize(), ONE);


        // Sell
        assertEquals(target.createOrder(key, builder.size(ONE.negate()).build()), future.get().getAcceptanceId());
        verify(orderService, times(2)).sendOrder(any());
        assertEquals(reference.get().getProduct(), key.getInstrument());
        assertEquals(reference.get().getType(), LIMIT);
        assertEquals(reference.get().getSide(), SELL);
        assertEquals(reference.get().getPrice(), TEN);
        assertEquals(reference.get().getSize(), ONE);

        // Market
        assertEquals(target.createOrder(key, builder.price(ZERO).build()), future.get().getAcceptanceId());
        verify(orderService, times(3)).sendOrder(any());
        assertEquals(reference.get().getProduct(), key.getInstrument());
        assertEquals(reference.get().getType(), MARKET);
        assertEquals(reference.get().getSide(), SELL);
        assertEquals(reference.get().getPrice(), ZERO);
        assertEquals(reference.get().getSize(), ONE);

        // Invalid Key
        assertNull(target.createOrder(null, builder.build()));
        assertNull(target.createOrder(Key.from(Request.builder().build()), builder.build()));
        verifyNoMoreInteractions(orderService);

        // Invalid Instruction
        assertNull(target.createOrder(key, null));
        assertNull(target.createOrder(key, builder.price(null).size(ONE).build()));
        assertNull(target.createOrder(key, builder.price(TEN).size(null).build()));
        verifyNoMoreInteractions(orderService);

    }

    @Test
    public void testCancelOrder() throws Exception {

        Key key = Key.from(Request.builder().instrument("inst").build());
        CancelInstruction.CancelInstructionBuilder builder = CancelInstruction.builder().id("aid");
        CompletableFuture<OrderCancel.Response> future = completedFuture(mock(OrderCancel.Response.class));
        AtomicReference<OrderCancel> reference = new AtomicReference<>();

        when(orderService.cancelOrder(any())).thenAnswer(i -> {

            reference.set(i.getArgumentAt(0, OrderCancel.class));

            return future;

        });

        assertSame(target.cancelOrder(key, builder.build()), "aid");
        verify(orderService, times(1)).cancelOrder(any());
        assertEquals(reference.get().getProduct(), key.getInstrument());
        assertEquals(reference.get().getAcceptanceId(), "aid");

        // Invalid Key
        assertNull(target.cancelOrder(null, builder.build()));
        assertNull(target.cancelOrder(Key.from(Request.builder().build()), builder.build()));
        verifyNoMoreInteractions(orderService);

        // Invalid Instruction
        assertNull(target.cancelOrder(key, null));
        assertNull(target.cancelOrder(key, builder.id(null).build()));
        verifyNoMoreInteractions(orderService);

    }

}
