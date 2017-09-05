package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import com.after_sunrise.cryptocurrency.bitflyer4j.Bitflyer4j;
import com.after_sunrise.cryptocurrency.bitflyer4j.entity.*;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.AccountService;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.MarketService;
import com.after_sunrise.cryptocurrency.bitflyer4j.service.OrderService;
import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ProductType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.after_sunrise.cryptocurrency.bitflyer4j.core.ConditionType.LIMIT;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.ConditionType.MARKET;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType.BUY;
import static com.after_sunrise.cryptocurrency.bitflyer4j.core.SideType.SELL;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.AssetType.COLLATERAL;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.AssetType.FUTURE_BTC1W;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer.BitflyerService.ProductType.BTCJPY_MAT1WK;
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

        target = spy(new BitflyerContext(module.getMock(Bitflyer4j.class)));

    }

    @Test
    public void testClose() throws Exception {

        Context context = new BitflyerContext();

        context.close();

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

        when(marketService.getTick(anyString())).thenReturn(null);
        assertNull(target.getMidPrice(key));
        target.clear();

        when(marketService.getTick(anyString())).thenReturn(completedFuture(null));
        assertNull(target.getMidPrice(key));
        target.clear();

        Tick tick = mock(Tick.class);
        when(marketService.getTick(anyString())).thenReturn(completedFuture(tick)).thenReturn(null);
        assertNull(target.getMidPrice(key));

        when(tick.getBestAskPrice()).thenReturn(null);
        when(tick.getBestBidPrice()).thenReturn(ONE);
        assertNull(target.getMidPrice(key));

        when(tick.getBestAskPrice()).thenReturn(TEN);
        when(tick.getBestBidPrice()).thenReturn(null);
        assertNull(target.getMidPrice(key));

        when(tick.getBestAskPrice()).thenReturn(TEN);
        when(tick.getBestBidPrice()).thenReturn(ONE);
        assertEquals(target.getMidPrice(key), new BigDecimal("5.5"));
        assertEquals(target.getMidPrice(key), new BigDecimal("5.5"));
        target.clear();
        assertNull(target.getMidPrice(key));
        assertNull(target.getMidPrice(key));

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
    public void testListTrades() {

        Key key = Key.from(Request.builder().instrument("inst").build());

        ZonedDateTime time = ZonedDateTime.now();

        Execution e1 = mock(Execution.class);
        Execution e2 = mock(Execution.class);
        Execution e3 = mock(Execution.class);
        Execution e4 = mock(Execution.class);
        Execution e5 = mock(Execution.class);
        Execution e6 = mock(Execution.class);
        Execution e7 = mock(Execution.class);
        when(e1.getTimestamp()).thenReturn(null);
        when(e2.getTimestamp()).thenReturn(time.minusSeconds(2));
        when(e3.getTimestamp()).thenReturn(time.minusSeconds(1));
        when(e4.getTimestamp()).thenReturn(time);
        when(e5.getTimestamp()).thenReturn(null);
        when(e6.getTimestamp()).thenReturn(time.plusSeconds(1));
        when(e7.getTimestamp()).thenReturn(time.plusSeconds(2));
        CompletableFuture<List<Execution>> f = completedFuture(asList(e1, e2, null, e3, e4, e5, e6, e7));
        when(marketService.getExecutions(eq("inst"), any())).thenReturn(f);

        List<Trade> results = target.listTrades(key, null);
        assertEquals(results.size(), 7);

        List<Trade> filtered = target.listTrades(key, time.toInstant());
        assertEquals(filtered.size(), 3);
        assertEquals(filtered.get(0).getTimestamp(), time.toInstant());
        assertEquals(filtered.get(1).getTimestamp(), time.plusSeconds(1).toInstant());
        assertEquals(filtered.get(2).getTimestamp(), time.plusSeconds(2).toInstant());

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
    public void testGetInstrumentPosition_Margin() throws Exception {

        Key key = Key.from(Request.builder().instrument("BTCJPY_MAT1WK").build());

        BigDecimal margin = ONE.add(ONE);

        doAnswer(i -> {

            assertSame(i.getArgumentAt(0, Key.class), key);

            assertSame(i.getArgumentAt(1, Function.class).apply(BTCJPY_MAT1WK), FUTURE_BTC1W);

            return margin;

        }).when(target).forMargin(any(), any());

        assertEquals(target.getInstrumentPosition(key), margin);

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
    public void testGetFundingPosition_Margin() throws Exception {

        Key key = Key.from(Request.builder().instrument("BTCJPY_MAT1WK").build());

        BigDecimal margin = ONE.add(ONE);

        doAnswer(i -> {

            assertSame(i.getArgumentAt(0, Key.class), key);

            assertSame(i.getArgumentAt(1, Function.class).apply(BTCJPY_MAT1WK), COLLATERAL);

            return margin;

        }).when(target).forMargin(any(), any());

        assertEquals(target.getFundingPosition(key), margin);

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
        assertNull(target.forBalance(key, ProductType::getStructure));
        verifyNoMoreInteractions(accountService);

        // No instrument
        key = Key.from(Request.builder().build());
        assertNull(target.forBalance(key, ProductType::getStructure));
        verifyNoMoreInteractions(accountService);

        // Unknown instrument
        key = Key.from(Request.builder().instrument("hoge").build());
        assertNull(target.forBalance(key, ProductType::getStructure));
        verifyNoMoreInteractions(accountService);

        // Found
        key = Key.from(Request.builder().instrument("BTC_JPY").build());
        assertEquals(target.forBalance(key, ProductType::getStructure), TEN);
        assertEquals(target.forBalance(key, ProductType::getStructure), TEN);

        // Next query
        target.clear();
        assertEquals(target.forBalance(key, ProductType::getStructure), TEN);
        assertEquals(target.forBalance(key, ProductType::getStructure), TEN);

        // Null result
        target.clear();
        assertNull(target.forBalance(key, ProductType::getStructure));
        assertNull(target.forBalance(key, ProductType::getStructure));

    }

    @Test
    public void testForMargin() {

        Product i1 = mock(Product.class);
        Product i2 = mock(Product.class);
        Product i3 = mock(Product.class);
        Product i4 = mock(Product.class);
        when(i1.getProduct()).thenReturn("BTCJPY08JAN2017");
        when(i2.getProduct()).thenReturn("BTCJPY14APR2017");
        when(i3.getProduct()).thenReturn("BTCJPY08OCT2017");
        when(i2.getAlias()).thenReturn("BTCJPY_MAT1WK");
        when(i3.getAlias()).thenReturn("BTCJPY_MAT2WK");
        when(i3.getAlias()).thenReturn("BTCJPY_MAT3WK");
        when(marketService.getProducts()).thenReturn(completedFuture(asList(i1, null, i2, i3, i4))).thenReturn(null);

        TradePosition.Response p1 = mock(TradePosition.Response.class);
        TradePosition.Response p2 = mock(TradePosition.Response.class);
        TradePosition.Response p3 = mock(TradePosition.Response.class);
        TradePosition.Response p4 = mock(TradePosition.Response.class);
        TradePosition.Response p5 = mock(TradePosition.Response.class);
        TradePosition.Response p6 = mock(TradePosition.Response.class);
        when(p2.getSide()).thenReturn(BUY); // No price
        when(p3.getSide()).thenReturn(BUY);
        when(p4.getSide()).thenReturn(SELL);
        when(p5.getSide()).thenReturn(SELL);
        when(p3.getSize()).thenReturn(TEN); // +10
        when(p4.getSize()).thenReturn(ONE); // -1
        when(p5.getSize()).thenReturn(ONE); // -1
        when(p6.getSize()).thenReturn(ONE); // No side
        when(orderService.listPositions(any())).thenAnswer(i -> {

            // Should be converted from "BTCJPY_MAT1WK" to "BTCJPY14APR2017"
            assertEquals(i.getArgumentAt(0, TradePosition.class).getProduct(), "BTCJPY14APR2017");

            return completedFuture(asList(p1, p2, p3, null, p4, p5, p6));

        }).thenReturn(null);

        Collateral c = mock(Collateral.class);
        when(c.getCollateral()).thenReturn(null, TEN);
        when(c.getRequiredCollateral()).thenReturn(ONE, null);
        when(accountService.getCollateral()).thenReturn(completedFuture(c)).thenReturn(null);

        Key key1 = Key.from(Request.builder().instrument("BTCJPY_MAT1WK").build());
        Key key2 = Key.from(Request.builder().instrument("BTC_JPY").build());
        Key key3 = Key.from(Request.builder().instrument("TEST").build());
        Key key4 = null;

        assertEquals(target.forMargin(key1, ProductType::getStructure), TEN.subtract(ONE).subtract(ONE));
        assertEquals(target.forMargin(key1, ProductType::getStructure), TEN.subtract(ONE).subtract(ONE));
        assertEquals(target.forMargin(key2, ProductType::getStructure), null);
        assertEquals(target.forMargin(key3, ProductType::getStructure), null);
        assertEquals(target.forMargin(key4, ProductType::getStructure), null);
        assertEquals(target.forMargin(key1, ProductType::getFunding), null); // No amount
        assertEquals(target.forMargin(key1, ProductType::getFunding), TEN.subtract(ONE)); // With Position
        assertEquals(target.forMargin(key1, ProductType::getFunding), null); // No P&L
        assertEquals(target.forMargin(key2, ProductType::getFunding), null);
        assertEquals(target.forMargin(key3, ProductType::getFunding), null);
        assertEquals(target.forMargin(key4, ProductType::getFunding), null);
        target.clear();
        assertEquals(target.forMargin(key1, ProductType::getStructure), ZERO);
        assertEquals(target.forMargin(key2, ProductType::getStructure), null);
        assertEquals(target.forMargin(key3, ProductType::getStructure), null);
        assertEquals(target.forMargin(key4, ProductType::getStructure), null);
        assertEquals(target.forMargin(key1, ProductType::getFunding), null);
        assertEquals(target.forMargin(key2, ProductType::getFunding), null);
        assertEquals(target.forMargin(key3, ProductType::getFunding), null);
        assertEquals(target.forMargin(key4, ProductType::getFunding), null);

    }

    @Test
    public void testConvertProductAlias() {

        List<Product> products = new ArrayList<>();
        products.add(mock(Product.class));
        products.add(mock(Product.class));
        products.add(null);
        products.add(mock(Product.class));
        products.add(mock(Product.class));
        when(products.get(0).getProduct()).thenReturn("BTCJPY08JAN2017");
        when(products.get(1).getProduct()).thenReturn("BTCJPY14APR2017");
        when(products.get(3).getProduct()).thenReturn("BTCJPY08OCT2017");
        when(products.get(4).getProduct()).thenReturn(null);
        when(products.get(0).getAlias()).thenReturn(null);
        when(products.get(1).getAlias()).thenReturn("BTCJPY_MAT1WK");
        when(products.get(3).getAlias()).thenReturn("BTCJPY_MAT2WK");
        when(products.get(4).getAlias()).thenReturn("BTCJPY_MAT3WK");

        when(marketService.getProducts()).thenReturn(completedFuture(products));

        Key.KeyBuilder b = Key.builder();
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY08JAN2017").build()), "BTCJPY08JAN2017");
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY14APR2017").build()), "BTCJPY14APR2017");
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY08OCT2017").build()), "BTCJPY08OCT2017");
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY_MAT1WK").build()), "BTCJPY14APR2017");
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY_MAT2WK").build()), "BTCJPY08OCT2017");
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY_MAT3WK").build()), null);
        assertEquals(target.convertProductAlias(b.instrument("TEST").build()), null);
        assertEquals(target.convertProductAlias(b.instrument(null).build()), null);
        assertEquals(target.convertProductAlias(null), null);
        reset(marketService);
        target.clear();
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY08JAN2017").build()), null);
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY14APR2017").build()), null);
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY08OCT2017").build()), null);
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY_MAT1WK").build()), null);
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY_MAT2WK").build()), null);
        assertEquals(target.convertProductAlias(b.instrument("BTCJPY_MAT3WK").build()), null);
        assertEquals(target.convertProductAlias(b.instrument("TEST").build()), null);
        assertEquals(target.convertProductAlias(b.instrument(null).build()), null);
        assertEquals(target.convertProductAlias(null), null);

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
    public void testGetCommissionRate() throws Exception {

        Key key = Key.from(Request.builder().instrument("i").build());

        CompletableFuture<TradeCommission.Response> f = completedFuture(mock(TradeCommission.Response.class));
        when(f.get().getRate()).thenReturn(ONE.movePointLeft(3));
        when(orderService.getCommission(any())).thenReturn(f).thenReturn(null);

        assertEquals(target.getCommissionRate(key), ONE.movePointLeft(3));
        assertEquals(target.getCommissionRate(key), ONE.movePointLeft(3));
        target.clear();
        assertEquals(target.getCommissionRate(key), null);
        assertEquals(target.getBestAskPrice(key), null);

    }

    @Test
    public void testIsMarginable() {

        Key.KeyBuilder builder = Key.builder();

        assertFalse(target.isMarginable(builder.build()));
        assertFalse(target.isMarginable(builder.instrument("FOO").build()));
        assertFalse(target.isMarginable(builder.instrument("JPY").build()));
        assertFalse(target.isMarginable(builder.instrument("BTC_JPY").build()));
        assertFalse(target.isMarginable(null));

        assertTrue(target.isMarginable(builder.instrument("FX_BTC_JPY").build()));
        assertTrue(target.isMarginable(builder.instrument("BTCJPY_MAT1WK").build()));
        assertTrue(target.isMarginable(builder.instrument("BTCJPY_MAT2WK").build()));

    }

    @Test
    public void testGetExpiry() {

        Key key = Key.builder().instrument("BTCJPY_MAT1WK").build();

        ZoneId zone = ZoneId.of("Asia/Tokyo");
        LocalTime time = LocalTime.of(11, 0);

        // Valid
        LocalDate date = LocalDate.of(2017, 9, 15);
        doReturn("BTCJPY15SEP2017").when(target).convertProductAlias(key);
        assertEquals(target.getExpiry(key), ZonedDateTime.of(date, time, zone));

        // Invalid (Sep 31)
        date = LocalDate.of(2017, 10, 1);
        doReturn("BTCJPY31SEP2017").when(target).convertProductAlias(key);
        assertEquals(target.getExpiry(key), ZonedDateTime.of(date, time, zone));

        // Invalid (Sep 00)
        date = LocalDate.of(2017, 8, 31);
        doReturn("BTCJPY00SEP2017").when(target).convertProductAlias(key);
        assertEquals(target.getExpiry(key), ZonedDateTime.of(date, time, zone));

        // Invalid Month (???)
        doReturn("BTCJPY15???2017").when(target).convertProductAlias(key);
        assertNull(target.getExpiry(key));

        // Pattern Mismatch
        doReturn("01SEP2017").when(target).convertProductAlias(key);
        assertNull(target.getExpiry(key));

        // Code not found
        doReturn(null).when(target).convertProductAlias(key);
        assertNull(target.getExpiry(key));

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

        List<Order> results = target.listActiveOrders(key);
        assertEquals(results.size(), 2);
        assertSame(results.get(0), o2);
        assertSame(results.get(1), o4);

    }

    @Test
    public void testListExecutions() {

        Key key = Key.from(Request.builder().instrument("inst").build());

        TradeExecution.Response r1 = mock(TradeExecution.Response.class);
        TradeExecution.Response r2 = mock(TradeExecution.Response.class);
        when(r1.getPrice()).thenReturn(ONE);
        when(r2.getPrice()).thenReturn(TEN);
        when(orderService.listExecutions(any(), any())).thenAnswer(i -> {

            assertEquals(i.getArgumentAt(0, TradeExecution.class).getProduct(), "inst");
            assertNull(i.getArgumentAt(0, TradeExecution.class).getChild_order_id());
            assertNull(i.getArgumentAt(0, TradeExecution.class).getChild_order_acceptance_id());
            assertNull(i.getArgumentAt(1, Pagination.class));

            return completedFuture(asList(r1, null, r2));

        }).thenReturn(null);

        // Queried
        List<Order.Execution> execs = target.listExecutions(key);
        assertEquals(execs.size(), 2);
        assertEquals(execs.get(0).getPrice(), ONE);
        assertEquals(execs.get(1).getPrice(), TEN);
        verify(orderService).listExecutions(any(), any());

        // Cached
        execs = target.listExecutions(key);
        assertEquals(execs.size(), 2);
        assertEquals(execs.get(0).getPrice(), ONE);
        assertEquals(execs.get(1).getPrice(), TEN);
        verify(orderService).listExecutions(any(), any());

        target.clear();

        // Queried
        execs = target.listExecutions(key);
        assertEquals(execs.size(), 0);
        verify(orderService, times(2)).listExecutions(any(), any());

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
