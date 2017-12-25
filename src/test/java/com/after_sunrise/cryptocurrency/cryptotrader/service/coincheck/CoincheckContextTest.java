package com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck.CoincheckContext.CurrencyType;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType.GET;
import static com.google.common.io.Resources.getResource;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.*;
import static java.math.RoundingMode.DOWN;
import static java.math.RoundingMode.UP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class CoincheckContextTest {

    private CoincheckContext target;

    private MapConfiguration conf;

    @BeforeMethod
    public void setUp() throws Exception {

        conf = new MapConfiguration(new HashMap<>());

        target = spy(new CoincheckContext());

        target.setConfiguration(conf);

        doReturn(null).when(target).request(any(), any(), any(), any());

    }

    @AfterMethod
    public void tearDown() throws Exception {
        target.close();
    }

    @Test(enabled = false)
    public void test() throws Exception {

        Path path = Paths.get(System.getProperty("user.home"), ".cryptotrader");
        target.setConfiguration(new Configurations().properties(path.toAbsolutePath().toFile()));

        doCallRealMethod().when(target).request(any(), any(), any(), any());

        Key key = Key.builder().instrument("btc_jpy").timestamp(Instant.now()).build();

        System.out.println("APX : " + target.getBestAskPrice(key));
        System.out.println("ASZ : " + target.getBestAskSize(key));
        System.out.println("BPX : " + target.getBestBidPrice(key));
        System.out.println("BSZ : " + target.getBestBidSize(key));
        System.out.println("MID : " + target.getMidPrice(key));
        System.out.println("LTP : " + target.getLastPrice(key));
        System.out.println("TRD : " + target.listTrades(key, null));

        System.out.println("CYI : " + target.getInstrumentCurrency(key));
        System.out.println("CYF : " + target.getFundingCurrency(key));
        System.out.println("CVB : " + target.getConversionPrice(key, CurrencyType.BTC));
        System.out.println("CVJ : " + target.getConversionPrice(key, CurrencyType.JPY));

        System.out.println("PSI : " + target.getInstrumentPosition(key));
        System.out.println("PSF : " + target.getFundingPosition(key));

        System.out.println("RIU : " + target.roundLotSize(key, new BigDecimal("1.23456"), UP));
        System.out.println("RID : " + target.roundLotSize(key, new BigDecimal("1.23456"), DOWN));
        System.out.println("RTU : " + target.roundTickSize(key, new BigDecimal("1.23456"), UP));
        System.out.println("RTD : " + target.roundTickSize(key, new BigDecimal("1.23456"), DOWN));
        System.out.println("COM : " + target.getCommissionRate(key));

        System.out.println("FDO : " + target.findOrder(key, "some_order_id"));
        System.out.println("LAO : " + target.listActiveOrders(key));
        System.out.println("LSE : " + target.listExecutions(key));

    }

    @Test(enabled = false)
    public void test_Order() throws Exception {

        Path path = Paths.get(System.getProperty("user.home"), ".cryptotrader");
        target.setConfiguration(new Configurations().properties(path.toAbsolutePath().toFile()));

        doCallRealMethod().when(target).request(any(), any(), any(), any());

        Key key = Key.builder().instrument("btc_jpy").timestamp(Instant.now()).build();

        Map<CreateInstruction, String> creates = target.createOrders(key, singleton(
                CreateInstruction.builder()
                        .price(new BigDecimal("1000000"))
                        .size(new BigDecimal("0.005")).build()
        ));

        creates.forEach((instruction, id) -> System.out.println(instruction + " -> " + id));

        TimeUnit.SECONDS.sleep(5);

        Map<CancelInstruction, String> cancels = target.cancelOrders(key, creates.values().stream()
                .filter(StringUtils::isNotEmpty)
                .map(id -> CancelInstruction.builder().id(id).build())
                .collect(Collectors.toSet())
        );

        cancels.forEach((instruction, id) -> System.out.println(instruction + " -> " + id));

    }

    @Test
    public void testGet() {
        assertEquals(target.get(), "coincheck");
    }

    @Test
    public void testQueryTick() throws Exception {

        String data = Resources.toString(getResource("json/coincheck_ticker.json"), UTF_8);
        doReturn(data).when(target).request(GET, "https://coincheck.com/api/ticker", null, null);

        // Found
        CoincheckTick tick = target.queryTick(Key.builder().instrument("btc_jpy").build()).get();
        assertEquals(tick.getAsk(), new BigDecimal("396000"));
        assertEquals(tick.getBid(), new BigDecimal("395835"));
        assertEquals(tick.getLast(), new BigDecimal("396000"));

        // Not found
        assertFalse(target.queryTick(Key.builder().instrument("FOO_BAR").build()).isPresent());

        // Cached
        doReturn(null).when(target).request(any(), any(), any(), any());
        CoincheckTick cached = target.queryTick(Key.builder().instrument("btc_jpy").build()).get();
        assertSame(cached, tick);

    }

    @Test
    public void testQueryBook() throws Exception {

        String data = Resources.toString(getResource("json/coincheck_book.json"), UTF_8);
        doReturn(data).when(target).request(GET, "https://coincheck.com/api/order_books", null, null);

        // Found
        CoincheckBook book = target.queryBook(Key.builder().instrument("btc_jpy").build()).get();
        assertEquals(book.getBestAskPrice(), new BigDecimal("1613281.0"));
        assertEquals(book.getBestBidPrice(), new BigDecimal("1613120.0"));
        assertEquals(book.getBestAskSize(), new BigDecimal("0.008"));
        assertEquals(book.getBestBidSize(), new BigDecimal("0.2032985"));

        // Not found
        assertFalse(target.queryBook(Key.builder().instrument("FOO_BAR").build()).isPresent());

        // Cached
        doReturn(null).when(target).request(any(), any(), any(), any());
        CoincheckBook cached = target.queryBook(Key.builder().instrument("btc_jpy").build()).get();
        assertSame(cached, book);

    }

    @Test
    public void testGetBestAskPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        CoincheckBook book = mock(CoincheckBook.class);
        when(book.getBestAskPrice()).thenReturn(TEN);

        doReturn(Optional.of(book)).when(target).queryBook(key);
        assertEquals(target.getBestAskPrice(key), book.getBestAskPrice());

        doReturn(Optional.empty()).when(target).queryBook(key);
        assertNull(target.getBestAskPrice(key));

    }

    @Test
    public void testGetBestBidPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        CoincheckBook book = mock(CoincheckBook.class);
        when(book.getBestBidPrice()).thenReturn(TEN);

        doReturn(Optional.of(book)).when(target).queryBook(key);
        assertEquals(target.getBestBidPrice(key), book.getBestBidPrice());

        doReturn(Optional.empty()).when(target).queryBook(key);
        assertNull(target.getBestBidPrice(key));

    }

    @Test
    public void testGetBestAskSize() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        CoincheckBook book = mock(CoincheckBook.class);
        when(book.getBestAskSize()).thenReturn(TEN);

        doReturn(Optional.of(book)).when(target).queryBook(key);
        assertEquals(target.getBestAskSize(key), book.getBestAskSize());

        doReturn(Optional.empty()).when(target).queryBook(key);
        assertNull(target.getBestAskSize(key));

    }

    @Test
    public void testGetBestBidSize() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        CoincheckBook book = mock(CoincheckBook.class);
        when(book.getBestBidSize()).thenReturn(TEN);

        doReturn(Optional.of(book)).when(target).queryBook(key);
        assertEquals(target.getBestBidSize(key), book.getBestBidSize());

        doReturn(Optional.empty()).when(target).queryBook(key);
        assertNull(target.getBestBidSize(key));

    }

    @Test
    public void testGetMidPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        doReturn(TEN).when(target).getBestAskPrice(key);
        doReturn(ONE).when(target).getBestBidPrice(key);
        assertEquals(target.getMidPrice(key), new BigDecimal("5.5"));

        doReturn(null).when(target).getBestAskPrice(key);
        doReturn(ONE).when(target).getBestBidPrice(key);
        assertEquals(target.getMidPrice(key), null);

        doReturn(TEN).when(target).getBestAskPrice(key);
        doReturn(null).when(target).getBestBidPrice(key);
        assertEquals(target.getMidPrice(key), null);

    }

    @Test
    public void testGetLastPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        CoincheckTick book = mock(CoincheckTick.class);
        when(book.getLast()).thenReturn(TEN);

        doReturn(Optional.of(book)).when(target).queryTick(key);
        assertEquals(target.getLastPrice(key), book.getLast());

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertNull(target.getLastPrice(key));

    }

    @Test
    public void testListTrades() throws Exception {

        Key key = Key.builder().instrument("btc_jpy").timestamp(Instant.parse("2017-08-02T00:00:00.000Z")).build();
        String data = Resources.toString(getResource("json/coincheck_trade.json"), UTF_8);
        doReturn(data).when(target).request(GET, "https://coincheck.com/api/trades?pair=btc_jpy&limit=500", null, null);

        // Found
        List<Trade> values = target.listTrades(key, null);
        assertEquals(values.size(), 3, values.toString());
        assertEquals(values.get(0).getTimestamp(), Instant.parse("2017-08-01T07:50:15.000Z"));
        assertEquals(values.get(0).getPrice(), new BigDecimal("320932"));
        assertEquals(values.get(0).getSize(), new BigDecimal("0.0065"));
        assertEquals(values.get(1).getTimestamp(), Instant.parse("2017-08-01T07:50:15.001Z"));
        assertEquals(values.get(1).getPrice(), new BigDecimal("320931"));
        assertEquals(values.get(1).getSize(), new BigDecimal("0.165828"));
        assertEquals(values.get(2).getTimestamp(), Instant.parse("2017-08-01T07:50:15.002Z"));
        assertEquals(values.get(2).getPrice(), new BigDecimal("320995"));
        assertEquals(values.get(2).getSize(), new BigDecimal("0.44"));

        // Not found
        List<Trade> unknown = target.listTrades(Key.builder().instrument("FOO").build(), null);
        assertEquals(unknown.size(), 0);

        // Cached
        doReturn(null).when(target).request(any(), any(), any(), any());
        List<Trade> cached = target.listTrades(key, null);
        assertEquals(cached, values);

        // Filtered
        List<Trade> filtered = target.listTrades(key, Instant.parse("2017-08-01T07:50:15.001Z"));
        assertEquals(filtered.size(), 2, filtered.toString());
        assertEquals(filtered.get(0), values.get(1));
        assertEquals(filtered.get(1), values.get(2));

    }

    @Test
    public void testGetInstrumentCurrency() {

        Key.KeyBuilder b = Key.builder();

        assertEquals(target.getInstrumentCurrency(b.instrument("btc_jpy").build()), CurrencyType.BTC);
        assertEquals(target.getInstrumentCurrency(b.instrument("foo").build()), null);
        assertEquals(target.getInstrumentCurrency(b.instrument(null).build()), null);

    }

    @Test
    public void testGetFundingCurrency() {

        Key.KeyBuilder b = Key.builder();

        assertEquals(target.getFundingCurrency(b.instrument("btc_jpy").build()), CurrencyType.JPY);
        assertEquals(target.getFundingCurrency(b.instrument("foo").build()), null);
        assertEquals(target.getFundingCurrency(b.instrument(null).build()), null);

    }

    @Test
    public void testGetConversionPrice() {

        Key key = Key.builder().instrument("btc_jpy").build();
        doReturn(TEN).when(target).getMidPrice(key);

        assertEquals(target.getConversionPrice(key, CurrencyType.BTC), ONE);
        assertEquals(target.getConversionPrice(key, CurrencyType.JPY), TEN);
        assertEquals(target.getConversionPrice(key, CurrencyType.ETH), null);

        key = Key.builder().instrument("FOO").build();
        assertEquals(target.getConversionPrice(key, CurrencyType.BTC), null);
        assertEquals(target.getConversionPrice(key, CurrencyType.JPY), null);
        assertEquals(target.getConversionPrice(key, CurrencyType.ETH), null);

    }

    @Test(timeOut = 60 * 1000L)
    public void testExecutePrivate() throws Exception {

        conf.addProperty(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck.CoincheckContext.api.id",
                "my_id"
        );
        conf.addProperty(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck.CoincheckContext.api.secret",
                "my_secret"
        );
        doReturn(Instant.ofEpochMilli(1234567890)).when(target).getNow();

        String path = "http://localhost:80/test";
        Map<String, String> params = ImmutableMap.of("foo", "bar");
        String data = "hoge";

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("ACCESS-KEY", "my_id");
        headers.put("ACCESS-NONCE", "1234567890");
        headers.put("ACCESS-SIGNATURE", "c1882128a3b8bcf13cec68d2dabf0ab867064f97afc90162624d32273a05b65a");
        doReturn("test").when(target).request(GET, path + "?foo=bar", headers, data);

        assertEquals(target.executePrivate(GET, path, params, data), "test");

    }


    @Test
    public void testQueryBalance() throws Exception {

        String data = Resources.toString(getResource("json/coincheck_balance.json"), UTF_8);
        doReturn(data).when(target).executePrivate(GET, "https://coincheck.com/api/accounts/balance", null, null);

        // Found
        CoincheckBalance balance = target.queryBalance(Key.builder().build()).get();
        assertEquals(balance.getJpy(), new BigDecimal("1234.0"));
        assertEquals(balance.getBtc(), new BigDecimal("0.1"));

        // Cached
        doReturn(null).when(target).executePrivate(any(), any(), any(), any());
        CoincheckBalance cached = target.queryBalance(Key.builder().build()).get();
        assertSame(cached, balance);

    }

    @Test
    public void testGetInstrumentPosition() {

        Key key = Key.builder().instrument("btc_jpy").build();
        CoincheckBalance balance = mock(CoincheckBalance.class);
        doReturn(TEN).when(balance).getBtc();

        doReturn(Optional.of(balance)).when(target).queryBalance(key);
        assertEquals(target.getInstrumentPosition(key), TEN);
        assertEquals(target.getInstrumentPosition(Key.builder().build()), null);

        doReturn(Optional.empty()).when(target).queryBalance(key);
        assertEquals(target.getInstrumentPosition(key), null);

    }

    @Test
    public void testGetFundingPosition() {

        Key key = Key.builder().instrument("btc_jpy").build();
        CoincheckBalance balance = mock(CoincheckBalance.class);
        doReturn(TEN).when(balance).getJpy();

        doReturn(Optional.of(balance)).when(target).queryBalance(key);
        assertEquals(target.getFundingPosition(key), TEN);
        assertEquals(target.getFundingPosition(Key.builder().build()), null);

        doReturn(Optional.empty()).when(target).queryBalance(key);
        assertEquals(target.getFundingPosition(key), null);

    }

    @Test
    public void testRoundLotSize() {

        Key key = Key.builder().instrument("btc_jpy").build();
        BigDecimal input = new BigDecimal("1.23456");

        assertEquals(target.roundLotSize(key, input, UP), new BigDecimal("1.235"));
        assertEquals(target.roundLotSize(key, input, DOWN), new BigDecimal("1.230"));
        assertNull(target.roundLotSize(Key.build(key).instrument("foo").build(), input, UP));
        assertNull(target.roundLotSize(key, null, UP));
        assertNull(target.roundLotSize(key, input, null));

        conf.addProperty(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck.CoincheckContext.size.lot",
                "0.00"
        );

        assertNull(target.roundLotSize(key, input, UP));
        assertNull(target.roundLotSize(key, input, DOWN));
        assertNull(target.roundLotSize(Key.build(key).instrument("foo").build(), input, UP));
        assertNull(target.roundLotSize(key, null, UP));
        assertNull(target.roundLotSize(key, input, null));

    }

    @Test
    public void testRoundTickSize() {

        Key key = Key.builder().instrument("btc_jpy").build();
        BigDecimal input = new BigDecimal("1.23456");

        assertEquals(target.roundTickSize(key, input, UP), new BigDecimal("2"));
        assertEquals(target.roundTickSize(key, input, DOWN), new BigDecimal("1"));
        assertNull(target.roundTickSize(Key.build(key).instrument("foo").build(), input, UP));
        assertNull(target.roundTickSize(key, null, UP));
        assertNull(target.roundTickSize(key, input, null));

        conf.addProperty(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck.CoincheckContext.size.tick",
                "0.00"
        );

        assertNull(target.roundTickSize(key, input, UP));
        assertNull(target.roundTickSize(key, input, DOWN));
        assertNull(target.roundTickSize(Key.build(key).instrument("foo").build(), input, UP));
        assertNull(target.roundTickSize(key, null, UP));
        assertNull(target.roundTickSize(key, input, null));

    }

    @Test
    public void testGetCommissionRate() {

        Key key = Key.builder().instrument("btc_jpy").build();
        assertEquals(target.getCommissionRate(key), ZERO);

        conf.addProperty(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck.CoincheckContext.commission.rate",
                "0.0001"
        );
        assertEquals(target.getCommissionRate(key), new BigDecimal("0.0001"));

        key = Key.build(key).instrument("foo").build();
        assertNull(target.getCommissionRate(key));

    }

    @Test
    public void testFetchOrders() throws Exception {

        String data = Resources.toString(getResource("json/coincheck_order.json"), UTF_8);
        doReturn(data).when(target).executePrivate(GET, "https://coincheck.com/api/exchange/orders/opens", null, null);

        // Found
        List<CoincheckOrder> orders = target.fetchOrders(Key.builder().build());
        assertEquals(orders.size(), 2, orders.toString());
        assertEquals(orders.get(0).getId(), "202835");
        assertEquals(orders.get(0).getProduct(), "btc_jpy");
        assertEquals(orders.get(0).getActive(), TRUE);
        assertEquals(orders.get(0).getOrderPrice(), new BigDecimal("26890"));
        assertEquals(orders.get(0).getOrderQuantity(), new BigDecimal("0.5527"));
        assertEquals(orders.get(0).getFilledQuantity(), null);
        assertEquals(orders.get(0).getRemainingQuantity(), new BigDecimal("0.5527"));
        assertEquals(orders.get(0).getSide(), "buy");
        assertEquals(orders.get(1).getId(), "202836");
        assertEquals(orders.get(1).getProduct(), "btc_jpy");
        assertEquals(orders.get(1).getActive(), TRUE);
        assertEquals(orders.get(1).getOrderPrice(), new BigDecimal("26990"));
        assertEquals(orders.get(1).getOrderQuantity(), new BigDecimal("-0.77"));
        assertEquals(orders.get(1).getFilledQuantity(), null);
        assertEquals(orders.get(1).getRemainingQuantity(), new BigDecimal("-0.77"));
        assertEquals(orders.get(1).getSide(), "sell");

        // Cached
        doReturn(null).when(target).executePrivate(any(), any(), any(), any());
        List<CoincheckOrder> cached = target.fetchOrders(Key.builder().build());
        assertEquals(cached, orders);

    }

    @Test
    public void testFindOrder() {

        List<CoincheckOrder> orders = IntStream.range(0, 4).mapToObj(i -> {
            CoincheckOrder order = mock(CoincheckOrder.class);
            when(order.getId()).thenReturn(String.valueOf(i));
            when(order.getProduct()).thenReturn("btc_jpy");
            return order;
        }).collect(Collectors.toList());

        when(orders.get(0).getId()).thenReturn(null);
        when(orders.get(1).getProduct()).thenReturn(null);
        when(orders.get(2).getProduct()).thenReturn("foo");

        Key key = Key.builder().instrument("btc_jpy").build();
        doReturn(orders).when(target).fetchOrders(key);

        assertNull(target.findOrder(key, "0"));
        assertNull(target.findOrder(key, "1"));
        assertNull(target.findOrder(key, "2"));
        assertEquals(target.findOrder(key, "3"), orders.get(3));

    }

    @Test
    public void testListActiveOrders() {

        List<CoincheckOrder> orders = IntStream.range(0, 5).mapToObj(i -> {
            CoincheckOrder order = mock(CoincheckOrder.class);
            when(order.getActive()).thenReturn(TRUE);
            when(order.getProduct()).thenReturn("btc_jpy");
            return order;
        }).collect(Collectors.toList());

        when(orders.get(1).getActive()).thenReturn(FALSE);
        when(orders.get(3).getProduct()).thenReturn("foo");

        Key key = Key.builder().instrument("btc_jpy").build();
        doReturn(orders).when(target).fetchOrders(key);

        List<Order> results = target.listActiveOrders(key);
        assertEquals(results.size(), 3);
        assertTrue(results.contains(orders.get(0)));
        assertTrue(results.contains(orders.get(2)));
        assertTrue(results.contains(orders.get(4)));

    }

    @Test
    public void testListExecutions() throws Exception {

        String data = Resources.toString(getResource("json/coincheck_transaction.json"), UTF_8);
        doReturn(data).when(target).executePrivate(GET, "https://coincheck.com/api/exchange/orders/transactions", null, null);

        // Found
        List<Order.Execution> executions = target.listExecutions(Key.builder().instrument("btc_jpy").build());
        assertEquals(executions.size(), 2, executions.toString());
        assertEquals(executions.get(0).getId(), "38");
        assertEquals(executions.get(0).getOrderId(), "49");
        assertEquals(executions.get(0).getTime(), Instant.parse("2015-11-18T07:02:21.000Z"));
        assertEquals(executions.get(0).getPrice(), new BigDecimal("40900.0"));
        assertEquals(executions.get(0).getSize(), new BigDecimal("0.1"));
        assertEquals(executions.get(1).getId(), "37");
        assertEquals(executions.get(1).getOrderId(), "48");
        assertEquals(executions.get(1).getTime(), Instant.parse("2015-11-18T07:02:20.000Z"));
        assertEquals(executions.get(1).getPrice(), new BigDecimal("40901.0"));
        assertEquals(executions.get(1).getSize(), new BigDecimal("-0.2"));

        // Cached
        doReturn(null).when(target).executePrivate(any(), any(), any(), any());
        List<Order.Execution> cached = target.listExecutions(Key.builder().instrument("btc_jpy").build());
        assertEquals(cached, executions);
    }

}
