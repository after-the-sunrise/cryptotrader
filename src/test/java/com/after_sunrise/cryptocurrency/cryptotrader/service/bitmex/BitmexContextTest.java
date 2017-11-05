package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexService.FundingType;
import com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexService.ProductType;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType.*;
import static com.google.common.io.Resources.getResource;
import static java.math.BigDecimal.*;
import static java.math.BigDecimal.valueOf;
import static java.math.RoundingMode.DOWN;
import static java.math.RoundingMode.UP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.*;
import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitmexContextTest {

    private BitmexContext target;

    @BeforeMethod
    public void setUp() throws Exception {

        target = spy(new BitmexContext());

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

        Key key = Key.builder().instrument("XBJZ17").timestamp(Instant.now()).build();

        // Tick
        System.out.println("Ask : " + target.getBestAskPrice(key));
        System.out.println("Bid : " + target.getBestBidPrice(key));
        System.out.println("Mid : " + target.getMidPrice(key));
        System.out.println("Ltp : " + target.getLastPrice(key));

        // Trade
        System.out.println("TRD : " + target.listTrades(key, null));

        // Account
        System.out.println("IPS : " + target.getInstrumentPosition(key));
        System.out.println("FPS : " + target.getFundingPosition(key));

        // Reference
        System.out.println("COM : " + target.getCommissionRate(key));
        System.out.println("MGN : " + target.isMarginable(key));
        System.out.println("EXP : " + target.getExpiry(key));

        // Order Query
        System.out.println("ORD : " + target.findOrders(key));
        System.out.println("EXC : " + target.listExecutions(key));

    }

    @Test(enabled = false)
    public void test_Order() throws Exception {

        Path path = Paths.get(System.getProperty("user.home"), ".cryptotrader");
        target.setConfiguration(new Configurations().properties(path.toAbsolutePath().toFile()));

        doCallRealMethod().when(target).request(any(), any(), any(), any());

        Key key = Key.builder().instrument("XBJZ17").timestamp(Instant.now()).build();

        Map<CreateInstruction, String> ids = target.createOrders(
                key, Collections.singleton(CreateInstruction.builder()
                        .price(new BigDecimal("700000"))
                        .size(new BigDecimal("1")).build()
                ));

        System.out.println("NEW : " + ids);

        Thread.sleep(TimeUnit.SECONDS.toMillis(3));

        ids.forEach((k, v) -> {
            System.out.println("CND : " + target.cancelOrders(key, Collections.singleton(
                    CancelInstruction.builder().id(v).build()
            )));
        });

    }

    @Test
    public void testConvertAlias() throws Exception {

        doReturn(Resources.toString(getResource("json/bitmex_alias.json"), UTF_8)).when(target)
                .request(GET, "https://www.bitmex.com/api/v1/instrument/activeIntervals", null, null);


        for (ProductType product : ProductType.values()) {

            String expect = null;

            switch (product) {
                case BXBT:
                case BXBT30M:
                case BXBTJPY:
                case BXBTJPY30M:
                    expect = "." + product.name();
                    break;
                case XBTUSD:
                    expect = "XBTUSD";
                    break;
                case XBT_QT:
                    expect = "XBTZ17";
                    break;
                case XBJ_QT:
                    expect = "XBJZ17";
                    break;
            }

            assertEquals(target.convertAlias(Key.builder().instrument(product.name()).build()), expect);

        }

        assertNull(target.convertAlias(Key.builder().instrument("foo").build()));
        assertNull(target.convertAlias(Key.builder().instrument(null).build()));
        assertNull(target.convertAlias(null));


    }

    @Test
    public void testQueryTick() throws Exception {

        doReturn(Resources.toString(getResource("json/bitmex_ticker.json"), UTF_8)).when(target)
                .request(GET, "https://www.bitmex.com/api/v1/instrument/activeAndIndices", null, null);

        Key key1 = Key.builder().instrument("XBTUSD").build();
        Key key2 = Key.builder().instrument("BXBT").build();
        Key key3 = Key.builder().instrument("XBT_QT").build();
        doReturn("XBTUSD").when(target).convertAlias(key1);
        doReturn(".BXBT").when(target).convertAlias(key2);
        doReturn("XBTZ17").when(target).convertAlias(key3);

        Optional<BitmexTick> result = target.queryTick(key1);
        assertTrue(result.isPresent());
        assertEquals(result.get().getSymbol(), "XBTUSD");
        assertEquals(result.get().getSettleCurrency(), "XBt");
        assertEquals(result.get().getState(), "Open");
        assertEquals(result.get().getLast(), new BigDecimal("6593.7"));
        assertEquals(result.get().getAsk(), new BigDecimal("6593.9"));
        assertEquals(result.get().getBid(), new BigDecimal("6593.8"));
        assertEquals(result.get().getMid(), new BigDecimal("6593.85"));
        assertEquals(result.get().getLotSize(), new BigDecimal("1"));
        assertEquals(result.get().getTickSize(), new BigDecimal("0.1"));
        assertEquals(result.get().getExpiry(), null);
        assertEquals(result.get().getReference(), ".BXBT");
        assertEquals(result.get().getMakerFee(), new BigDecimal("-0.00025"));
        assertEquals(result.get().getTakerFee(), new BigDecimal("0.00075"));
        assertEquals(result.get().getSettleFee(), new BigDecimal("0"));

        result = target.queryTick(key2);
        assertTrue(result.isPresent());
        assertEquals(result.get().getSymbol(), ".BXBT");
        assertEquals(result.get().getSettleCurrency(), "");
        assertEquals(result.get().getState(), "Unlisted");
        assertEquals(result.get().getLast(), new BigDecimal("6601.72"));
        assertEquals(result.get().getAsk(), null);
        assertEquals(result.get().getBid(), null);
        assertEquals(result.get().getMid(), null);
        assertEquals(result.get().getLotSize(), null);
        assertEquals(result.get().getTickSize(), new BigDecimal("0.01"));
        assertEquals(result.get().getExpiry(), null);
        assertEquals(result.get().getReference(), ".BXBT");
        assertEquals(result.get().getMakerFee(), null);
        assertEquals(result.get().getTakerFee(), null);
        assertEquals(result.get().getSettleFee(), null);

        result = target.queryTick(key3);
        assertTrue(result.isPresent());
        assertEquals(result.get().getSymbol(), "XBTZ17");
        assertEquals(result.get().getSettleCurrency(), "XBt");
        assertEquals(result.get().getState(), "Open");
        assertEquals(result.get().getLast(), new BigDecimal("6712.3"));
        assertEquals(result.get().getAsk(), new BigDecimal("6712.5"));
        assertEquals(result.get().getBid(), new BigDecimal("6712.4"));
        assertEquals(result.get().getMid(), new BigDecimal("6712.45"));
        assertEquals(result.get().getLotSize(), new BigDecimal("1"));
        assertEquals(result.get().getTickSize(), new BigDecimal("0.1"));
        assertEquals(result.get().getExpiry(), Instant.parse("2017-12-29T12:00:00.000Z"));
        assertEquals(result.get().getReference(), ".BXBT30M");
        assertEquals(result.get().getMakerFee(), new BigDecimal("-0.00025"));
        assertEquals(result.get().getTakerFee(), new BigDecimal("0.00075"));
        assertEquals(result.get().getSettleFee(), new BigDecimal("0.0005"));

        // Empty
        target.clear();
        doReturn(null).when(target).request(any(), any(), any(), any());
        assertFalse(target.queryTick(key1).isPresent());

        // Exception
        target.clear();
        doThrow(new IOException("test")).when(target).request(any(), any(), any(), any());
        assertFalse(target.queryTick(key1).isPresent());

    }

    @Test
    public void testGetBestAskPrice() throws Exception {

        Key key = Key.builder().build();

        doReturn(of(BitmexTick.builder().ask(TEN).build())).when(target).queryTick(key);
        assertEquals(target.getBestAskPrice(key), TEN);

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertEquals(target.getBestAskPrice(key), null);

    }

    @Test
    public void testGetBestBidPrice() throws Exception {

        Key key = Key.builder().build();

        doReturn(of(BitmexTick.builder().bid(TEN).build())).when(target).queryTick(key);
        assertEquals(target.getBestBidPrice(key), TEN);

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertEquals(target.getBestBidPrice(key), null);

    }

    @Test
    public void testGetMidPrice() throws Exception {

        Key key = Key.builder().build();

        doReturn(of(BitmexTick.builder().mid(TEN).build())).when(target).queryTick(key);
        assertEquals(target.getMidPrice(key), TEN);

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertEquals(target.getMidPrice(key), null);

    }

    @Test
    public void testGetLastPrice() throws Exception {

        Key key = Key.builder().build();

        doReturn(of(BitmexTick.builder().last(TEN).build())).when(target).queryTick(key);
        assertEquals(target.getLastPrice(key), TEN);

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertEquals(target.getLastPrice(key), null);

    }

    @Test
    public void testListTrades() throws Exception {

        doReturn(Resources.toString(getResource("json/bitmex_trade.json"), UTF_8))
                .when(target).request(GET,
                "https://www.bitmex.com/api/v1/trade?count=500&reverse=true&symbol=XBTZ17", null, null);

        Key key = Key.builder().instrument("XBT_QT").build();
        doReturn("XBTZ17").when(target).convertAlias(key);

        List<Trade> trades = target.listTrades(key, null);
        assertEquals(trades.size(), 2);

        assertEquals(trades.get(0).getPrice(), new BigDecimal("6601.7"));
        assertEquals(trades.get(0).getSize(), new BigDecimal("686"));
        assertEquals(trades.get(0).getTimestamp(), Instant.parse("2017-11-01T22:15:47.303Z"));
        assertEquals(trades.get(0).getBuyOrderId(), "f391ff88-6731-f02d-46c2-b82471e762a9");
        assertEquals(trades.get(0).getSellOrderId(), "f391ff88-6731-f02d-46c2-b82471e762a9");

        assertEquals(trades.get(1).getPrice(), new BigDecimal("6601.6"));
        assertEquals(trades.get(1).getSize(), new BigDecimal("5"));
        assertEquals(trades.get(1).getTimestamp(), Instant.parse("2017-11-01T22:15:47.303Z"));
        assertEquals(trades.get(1).getBuyOrderId(), "bb1d629e-959c-298f-bab4-2921218193fa");
        assertEquals(trades.get(1).getSellOrderId(), "bb1d629e-959c-298f-bab4-2921218193fa");

        // Filtered in
        trades = target.listTrades(key, Instant.ofEpochMilli(0));
        assertEquals(trades.size(), 2);

        // Filtered out
        trades = target.listTrades(key, Instant.now());
        assertEquals(trades.size(), 0);

        // No data
        trades = target.listTrades(Key.builder().instrument("XBJ_QT").build(), null);
        assertEquals(trades.size(), 0);

        // Null key
        trades = target.listTrades(null, null);
        assertEquals(trades.size(), 0);

    }

    @Test
    public void testComputeHash() throws Exception {

        // Samples from : https://www.bitmex.com/app/apiKeysUsage
        String secret = "chNOOS4KvNXR_Xq4k4c9qsfoKWvnDecLATCRlcBwyKDYnWgO";

        // GET
        String path = "/api/v1/instrument?filter=%7B%22symbol%22%3A+%22XBTM15%22%7D";
        String verb = "GET";
        String nonce = "1429631577690";
        String data = null;
        assertEquals(target.computeHash(secret, verb, path, nonce, data),
                "9f1753e2db64711e39d111bc2ecace3dc9e7f026e6f65b65c4f53d3d14a60e5f");

        // POST
        path = "/api/v1/order";
        verb = "POST";
        nonce = "1429631577995";
        data = "{'symbol':'XBTM15','price':219.0,'clOrdID':'mm_bitmex_1a/oemUeQ4CAJZgP3fjHsA','orderQty':98}"
                .replaceAll("'", "\"");
        assertEquals(target.computeHash(secret, verb, path, nonce, data),
                "93912e048daa5387759505a76c28d6e92c6a0d782504fc9980f4fb8adfc13e25");

    }

    @Test
    public void testExecutePrivate() throws Exception {

        String path = "/hoge";
        String data = "test data";
        String body = "test body";

        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("k?", "v&");
        parameters.put(null, "v ");
        parameters.put("k/", null);
        parameters.put("k:", "v;");

        doReturn("my_id").when(target).getStringProperty(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexContext.api.id", null
        );
        doReturn("my_secret").when(target).getStringProperty(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexContext.api.secret", null
        );
        doReturn(Instant.ofEpochMilli(12345)).when(target).getNow();

        doAnswer(i -> {

            assertEquals(i.getArgumentAt(0, RequestType.class), PUT);
            assertEquals(i.getArgumentAt(1, String.class), "https://www.bitmex.com/hoge?k%3F=v%26&k%3A=v%3B");
            assertEquals(i.getArgumentAt(3, String.class), "test data");

            Map<?, ?> headers = i.getArgumentAt(2, Map.class);
            assertEquals(headers.remove("Content-Type"), "application/json");
            assertEquals(headers.remove("api-nonce"), "12345");
            assertEquals(headers.remove("api-key"), "my_id");
            assertEquals(headers.remove("api-signature"),
                    "6ef0c2129a11f4dcd8d92ddfa35481a923767e98bbcdd075718dde54e600017a");
            assertEquals(headers.size(), 0, headers.toString());

            return body;

        }).when(target).request(any(), any(), any(), any());

        // Proper request
        assertEquals(target.executePrivate(PUT, path, parameters, data), body);

        // Tokens not configured
        doReturn(null).when(target).getStringProperty(any(), any());
        assertNull(target.executePrivate(PUT, path, parameters, data));

    }

    @Test
    public void testGetInstrumentPosition() throws Exception {

        doReturn(Resources.toString(getResource("json/bitmex_position.json"), UTF_8))
                .when(target).executePrivate(GET, "/api/v1/position", null, null);

        Key key1 = Key.builder().instrument("XBT_QT").build();
        Key key2 = Key.builder().instrument("XBJ_QT").build();
        doReturn("XBTZ17").when(target).convertAlias(key1);
        doReturn("XBJZ17").when(target).convertAlias(key2);

        // Long
        assertEquals(target.getInstrumentPosition(key1), new BigDecimal("100"));
        verify(target, times(1)).executePrivate(any(), any(), any(), any());

        // Short
        assertEquals(target.getInstrumentPosition(key2), new BigDecimal("-100"));
        verify(target, times(1)).executePrivate(any(), any(), any(), any());

        // No match
        assertEquals(target.getInstrumentPosition(Key.builder().build()), ZERO);
        verify(target, times(1)).executePrivate(any(), any(), any(), any());

        // No data
        doReturn(null).when(target).executePrivate(any(), any(), any(), any());
        target.clear();
        assertEquals(target.getInstrumentPosition(key2), null);
        verify(target, times(2)).executePrivate(any(), any(), any(), any());

        // Error
        doThrow(new IOException("test")).when(target).executePrivate(any(), any(), any(), any());
        target.clear();
        assertEquals(target.getInstrumentPosition(key2), null);
        verify(target, times(3)).executePrivate(any(), any(), any(), any());

    }

    @Test
    public void testGetFundingPosition() throws Exception {

        doReturn(Resources.toString(getResource("json/bitmex_margin.json"), UTF_8))
                .when(target).executePrivate(GET, "/api/v1/user/margin", singletonMap("currency", "all"), null);

        Key key1 = Key.builder().instrument("XBT_QT").build();
        Key key2 = Key.builder().instrument("XBJ_QT").build();

        doReturn(Optional.empty()).when(target).queryTick(any());
        doReturn(of(BitmexTick.builder().settleCurrency("XBt").build())).when(target).queryTick(key1);
        doReturn(of(BitmexTick.builder().settleCurrency("XBt").build())).when(target).queryTick(key2);

        doReturn(null).when(target).getFundingConversionRate(any(), any());
        doReturn(new BigDecimal("6500.12")).when(target).getFundingConversionRate(key1, FundingType.XBT);
        doReturn(new BigDecimal("750000")).when(target).getFundingConversionRate(key2, FundingType.XBT);

        // Found (margin = 49990819 SATOSHI)
        assertEquals(target.getFundingPosition(key1), new BigDecimal("3249.4632239828"));
        assertEquals(target.getFundingPosition(key2), new BigDecimal("374931.14250000"));
        verify(target, times(1)).executePrivate(any(), any(), any(), any());

        // No data
        doReturn(null).when(target).executePrivate(any(), any(), any(), any());
        target.clear();
        assertEquals(target.getFundingPosition(key1), null);
        assertEquals(target.getFundingPosition(key2), null);
        verify(target, times(2)).executePrivate(any(), any(), any(), any());

        // Error
        doThrow(new IOException("test")).when(target).executePrivate(any(), any(), any(), any());
        target.clear();
        assertEquals(target.getFundingPosition(key1), null);
        assertEquals(target.getFundingPosition(key2), null);
        verify(target, times(4)).executePrivate(any(), any(), any(), any());

        // No rate
        doReturn(null).when(target).getFundingConversionRate(any(), any());
        target.clear();
        assertEquals(target.getFundingPosition(key1), null);
        assertEquals(target.getFundingPosition(key2), null);
        verify(target, times(4)).executePrivate(any(), any(), any(), any());

    }

    @Test
    public void testRoundLotSize() throws Exception {

        Key key = Key.builder().build();

        // OK
        doReturn(of(BitmexTick.builder().lotSize(TEN).build())).when(target).queryTick(key);
        assertEquals(target.roundLotSize(key, valueOf(5), UP), TEN);
        assertEquals(target.roundLotSize(key, valueOf(5), DOWN), ZERO);

        // Null input
        assertEquals(target.roundLotSize(key, null, UP), null);
        assertEquals(target.roundLotSize(key, valueOf(5), null), null);

        // Zero lot
        doReturn(of(BitmexTick.builder().lotSize(ZERO).build())).when(target).queryTick(key);
        assertEquals(target.roundLotSize(key, valueOf(5), UP), null);
        assertEquals(target.roundLotSize(key, valueOf(5), DOWN), null);

        // Null lot
        doReturn(of(BitmexTick.builder().build())).when(target).queryTick(key);
        assertEquals(target.roundLotSize(key, valueOf(5), UP), null);
        assertEquals(target.roundLotSize(key, valueOf(5), DOWN), null);

        // Null tick
        doReturn(Optional.empty()).when(target).queryTick(key);
        assertEquals(target.roundLotSize(key, valueOf(5), UP), null);
        assertEquals(target.roundLotSize(key, valueOf(5), DOWN), null);

    }

    @Test
    public void testRoundTickSize() throws Exception {

        Key key = Key.builder().build();

        // OK
        doReturn(of(BitmexTick.builder().tickSize(TEN).build())).when(target).queryTick(key);
        assertEquals(target.roundTickSize(key, valueOf(15), UP), TEN.add(TEN));
        assertEquals(target.roundTickSize(key, valueOf(15), DOWN), TEN);

        // Rounded to zero
        assertEquals(target.roundTickSize(key, valueOf(+9), DOWN), null);
        assertEquals(target.roundTickSize(key, valueOf(-9), DOWN), null);

        // Null input
        assertEquals(target.roundTickSize(key, null, UP), null);
        assertEquals(target.roundTickSize(key, valueOf(15), null), null);

        // Zero tick
        doReturn(of(BitmexTick.builder().tickSize(ZERO).build())).when(target).queryTick(key);
        assertEquals(target.roundTickSize(key, valueOf(15), UP), null);
        assertEquals(target.roundTickSize(key, valueOf(15), DOWN), null);

        // Null tick
        doReturn(of(BitmexTick.builder().build())).when(target).queryTick(key);
        assertEquals(target.roundTickSize(key, valueOf(15), UP), null);
        assertEquals(target.roundTickSize(key, valueOf(15), DOWN), null);

        // Null tick
        doReturn(Optional.empty()).when(target).queryTick(key);
        assertEquals(target.roundTickSize(key, valueOf(15), UP), null);
        assertEquals(target.roundTickSize(key, valueOf(15), DOWN), null);

    }

    @Test
    public void testGetCommissionRate() throws Exception {

        Key key = Key.builder().build();

        // With commission
        doReturn(of(BitmexTick.builder().makerFee(ONE).settleFee(TEN).build())).when(target).queryTick(key);
        assertEquals(target.getCommissionRate(key), ONE.add(TEN));

        // Without settle
        doReturn(of(BitmexTick.builder().makerFee(ONE).build())).when(target).queryTick(key);
        assertEquals(target.getCommissionRate(key), ONE);

        // Without maker
        doReturn(of(BitmexTick.builder().settleFee(TEN).build())).when(target).queryTick(key);
        assertEquals(target.getCommissionRate(key), TEN);

        // Null commission
        doReturn(of(BitmexTick.builder().build())).when(target).queryTick(key);
        assertEquals(target.getCommissionRate(key), ZERO);

        // Null tick
        doReturn(Optional.empty()).when(target).queryTick(key);
        assertEquals(target.getCommissionRate(key), null);

    }

    @Test
    public void testIsMarginable() throws Exception {
        assertTrue(target.isMarginable(null));
    }

    @Test
    public void testGetExpiry() throws Exception {

        Key key = Key.builder().build();

        // With expiry
        Instant expiry = Instant.now();
        doReturn(of(BitmexTick.builder().expiry(expiry).build())).when(target).queryTick(key);
        assertEquals(target.getExpiry(key).toInstant(), expiry);

        // No expiry
        doReturn(of(BitmexTick.builder().build())).when(target).queryTick(key);
        assertEquals(target.getExpiry(key), null);

        // Null tick
        doReturn(Optional.empty()).when(target).queryTick(key);
        assertEquals(target.getExpiry(key), null);

    }

    @Test
    public void testFindOrders() throws Exception {

        Map<String, String> parameters = new HashMap<>();
        parameters.put("count", "500");
        parameters.put("reverse", "true");
        parameters.put("symbol", "XBTZ17");
        doReturn(Resources.toString(getResource("json/bitmex_order.json"), UTF_8))
                .when(target).executePrivate(GET, "/api/v1/order", parameters, null);

        Key key = Key.builder().instrument("XBT_QT").build();
        doReturn("XBTZ17").when(target).convertAlias(key);

        List<BitmexOrder> orders = target.findOrders(key);
        assertEquals(orders.size(), 2);

        assertEquals(orders.get(0).getOrderId(), "94ed7a64-58de-172e-c0cc-1d1011b9e505");
        assertEquals(orders.get(0).getClientId(), "");
        assertEquals(orders.get(0).getActive(), Boolean.FALSE);
        assertEquals(orders.get(0).getProduct(), "XBTZ17");
        assertEquals(orders.get(0).getSide(), "Buy");
        assertEquals(orders.get(0).getOrderPrice(), new BigDecimal("6150"));
        assertEquals(orders.get(0).getQuantity(), new BigDecimal("9"));
        assertEquals(orders.get(0).getFilled(), new BigDecimal("9"));
        assertEquals(orders.get(0).getRemaining(), new BigDecimal("0"));
        assertEquals(orders.get(0).getId(), "94ed7a64-58de-172e-c0cc-1d1011b9e505");
        assertEquals(orders.get(0).getOrderQuantity(), new BigDecimal("9"));
        assertEquals(orders.get(0).getFilledQuantity(), new BigDecimal("9"));
        assertEquals(orders.get(0).getRemainingQuantity(), new BigDecimal("0"));

        assertEquals(orders.get(1).getOrderId(), "3f776031-32b2-2546-607c-07b1f14e70b3");
        assertEquals(orders.get(1).getClientId(), "my_order_id");
        assertEquals(orders.get(1).getActive(), Boolean.TRUE);
        assertEquals(orders.get(1).getProduct(), "XBTZ17");
        assertEquals(orders.get(1).getSide(), "Sell");
        assertEquals(orders.get(1).getOrderPrice(), new BigDecimal("6150"));
        assertEquals(orders.get(1).getQuantity(), new BigDecimal("9"));
        assertEquals(orders.get(1).getFilled(), new BigDecimal("8"));
        assertEquals(orders.get(1).getRemaining(), new BigDecimal("1"));
        assertEquals(orders.get(1).getId(), "my_order_id");
        assertEquals(orders.get(1).getOrderQuantity(), new BigDecimal("-9"));
        assertEquals(orders.get(1).getFilledQuantity(), new BigDecimal("-8"));
        assertEquals(orders.get(1).getRemainingQuantity(), new BigDecimal("-1"));

        doReturn(null).when(target).executePrivate(any(), any(), any(), any());
        target.clear();
        assertEquals(target.findOrders(Key.builder().instrument("XBTZ17").build()).size(), 0);

    }

    @Test
    public void testFindOrder() throws Exception {

        Key key = Key.builder().build();

        List<BitmexOrder> orders = new ArrayList<>();
        orders.add(BitmexOrder.builder().build());
        orders.add(BitmexOrder.builder().orderId("oid").build());
        orders.add(BitmexOrder.builder().clientId("cid").build());
        doReturn(orders).when(target).findOrders(key);

        assertSame(target.findOrder(key, "oid"), orders.get(1));
        assertSame(target.findOrder(key, "cid"), orders.get(2));
        assertNull(target.findOrder(key, "foo"));
        assertNull(target.findOrder(key, null));

    }

    @Test
    public void testListActiveOrders() throws Exception {

        Key key = Key.builder().build();

        List<BitmexOrder> orders = new ArrayList<>();
        orders.add(BitmexOrder.builder().active(null).build());
        orders.add(BitmexOrder.builder().active(true).build());
        orders.add(BitmexOrder.builder().active(false).build());
        orders.add(BitmexOrder.builder().active(true).build());
        doReturn(orders).when(target).findOrders(key);

        List<Order> result = target.listActiveOrders(key);
        assertEquals(result.size(), 2);
        assertSame(result.get(0), orders.get(1));
        assertSame(result.get(1), orders.get(3));

    }

    @Test
    public void testListExecutions() throws Exception {

        Map<String, String> parameters = new HashMap<>();
        parameters.put("count", "500");
        parameters.put("reverse", "true");
        parameters.put("symbol", "XBTZ17");
        doReturn(Resources.toString(getResource("json/bitmex_execution.json"), UTF_8))
                .when(target).executePrivate(GET, "/api/v1/execution/tradeHistory", parameters, null);

        Key key = Key.builder().instrument("XBT_QT").build();
        doReturn("XBTZ17").when(target).convertAlias(key);

        List<Order.Execution> executions = target.listExecutions(key);
        assertEquals(executions.size(), 2);

        assertEquals(executions.get(0).getId(), "ca5a97aa-a1bc-0629-3f11-b1937fd1bd3a");
        assertEquals(executions.get(0).getOrderId(), "3f776031-32b2-2546-607c-07b1f14e70b3");
        assertEquals(executions.get(0).getTime(), Instant.parse("2017-11-01T12:42:39.566Z"));
        assertEquals(executions.get(0).getPrice(), new BigDecimal("6150"));
        assertEquals(executions.get(0).getSize(), new BigDecimal("1"));

        assertEquals(executions.get(1).getId(), "ca5a97aa-a1bc-0629-3f11-b1937fd1bd3Z");
        assertEquals(executions.get(1).getOrderId(), "my_order_id");
        assertEquals(executions.get(1).getTime(), Instant.parse("2017-11-01T12:42:39.566Z"));
        assertEquals(executions.get(1).getPrice(), new BigDecimal("6150"));
        assertEquals(executions.get(1).getSize(), new BigDecimal("-1"));

        doReturn(null).when(target).executePrivate(any(), any(), any(), any());
        target.clear();
        assertEquals(target.listExecutions(Key.builder().instrument("XBTZ17").build()).size(), 0);

    }

    @Test
    public void testCreateOrders_Buy() throws Exception {

        Key key = Key.builder().instrument("XBT_QT").build();
        doReturn("XBTZ17").when(target).convertAlias(key);
        doReturn("uid1").when(target).getUniqueId();

        doAnswer(i -> {

            assertEquals(i.getArgumentAt(0, RequestType.class), POST);
            assertEquals(i.getArgumentAt(1, String.class), "/api/v1/order");
            assertEquals(i.getArgumentAt(2, Map.class), emptyMap());
            String data = i.getArgumentAt(3, String.class);

            Map<String, String> map = new Gson().fromJson(data, new TypeToken<Map<String, String>>() {
            }.getType());
            assertEquals(map.remove("clOrdID"), "uid1");
            assertEquals(map.remove("execInst"), "ParticipateDoNotInitiate");
            assertEquals(map.remove("ordType"), "Limit");
            assertEquals(map.remove("orderQty"), "10");
            assertEquals(map.remove("price"), "1");
            assertEquals(map.remove("side"), "Buy");
            assertEquals(map.remove("symbol"), "XBTZ17");
            assertEquals(map.size(), 0, map.toString());

            return new Gson().toJson(singletonMap("clOrdID", "cid1"));

        }).when(target).executePrivate(any(), any(), any(), any());

        CreateInstruction i1 = CreateInstruction.builder().price(ZERO).size(TEN).build();
        CreateInstruction i2 = CreateInstruction.builder().price(null).size(TEN).build();
        CreateInstruction i3 = CreateInstruction.builder().price(ONE).size(TEN).build(); // Valid
        CreateInstruction i4 = CreateInstruction.builder().price(ONE).size(ZERO).build();
        CreateInstruction i5 = CreateInstruction.builder().price(ONE).size(null).build();
        Set<CreateInstruction> instructions = Sets.newHashSet(i1, i2, null, i3, i4, i5);

        Map<CreateInstruction, String> result = target.createOrders(key, instructions);
        assertEquals(result.size(), 5);
        assertEquals(result.get(i1), null);
        assertEquals(result.get(i2), null);
        assertEquals(result.get(i3), "cid1");
        assertEquals(result.get(i4), null);
        assertEquals(result.get(i5), null);

    }

    @Test
    public void testCreateOrders_Sell() throws Exception {

        Key key = Key.builder().instrument("XBT_QT").build();
        doReturn("XBTZ17").when(target).convertAlias(key);
        doReturn("uid1").when(target).getUniqueId();

        doAnswer(i -> {

            assertEquals(i.getArgumentAt(0, RequestType.class), POST);
            assertEquals(i.getArgumentAt(1, String.class), "/api/v1/order");
            assertEquals(i.getArgumentAt(2, Map.class), emptyMap());
            String data = i.getArgumentAt(3, String.class);

            Map<String, String> map = new Gson().fromJson(data, new TypeToken<Map<String, String>>() {
            }.getType());
            assertEquals(map.remove("clOrdID"), "uid1");
            assertEquals(map.remove("execInst"), "ParticipateDoNotInitiate");
            assertEquals(map.remove("ordType"), "Limit");
            assertEquals(map.remove("orderQty"), "10");
            assertEquals(map.remove("price"), "1");
            assertEquals(map.remove("side"), "Sell");
            assertEquals(map.remove("symbol"), "XBTZ17");
            assertEquals(map.size(), 0, map.toString());

            return new Gson().toJson(singletonMap("clOrdID", "cid1"));

        }).when(target).executePrivate(any(), any(), any(), any());

        CreateInstruction i1 = CreateInstruction.builder().price(ZERO).size(TEN).build();
        CreateInstruction i2 = CreateInstruction.builder().price(null).size(TEN).build();
        CreateInstruction i3 = CreateInstruction.builder().price(ONE).size(TEN.negate()).build(); // Valid
        CreateInstruction i4 = CreateInstruction.builder().price(ONE).size(ZERO).build();
        CreateInstruction i5 = CreateInstruction.builder().price(ONE).size(null).build();
        Set<CreateInstruction> instructions = Sets.newHashSet(i1, i2, null, i3, i4, i5);

        Map<CreateInstruction, String> result = target.createOrders(key, instructions);
        assertEquals(result.size(), 5);
        assertEquals(result.get(i1), null);
        assertEquals(result.get(i2), null);
        assertEquals(result.get(i3), "cid1");
        assertEquals(result.get(i4), null);
        assertEquals(result.get(i5), null);

    }

    @Test
    public void testCancelOrders() throws Exception {

        doAnswer(i -> {

            assertEquals(i.getArgumentAt(0, RequestType.class), DELETE);
            assertEquals(i.getArgumentAt(1, String.class), "/api/v1/order");
            assertEquals(i.getArgumentAt(2, Map.class), emptyMap());
            String data = i.getArgumentAt(3, String.class);

            Map<String, String> map = new Gson().fromJson(data, new TypeToken<Map<String, String>>() {
            }.getType());
            assertEquals(map.remove("clOrdID"), "uid1");

            return new Gson().toJson(singleton(singletonMap("clOrdID", "cid1")));

        }).when(target).executePrivate(any(), any(), any(), any());

        CancelInstruction i1 = CancelInstruction.builder().id(null).build();
        CancelInstruction i2 = CancelInstruction.builder().id("uid1").build();

        Key key = Key.builder().instrument("XBTZ17").build();
        Map<CancelInstruction, String> result = target.cancelOrders(key, Sets.newHashSet(i1, null, i2));
        assertEquals(result.size(), 2);
        assertEquals(result.get(i1), null);
        assertEquals(result.get(i2), "cid1");

    }

}
