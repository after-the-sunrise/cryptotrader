package com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexService.ProductType;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.configuration2.Configuration;
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

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.*;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexService.FundingType.XBT;
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

    private Configuration configuration;

    @BeforeMethod
    public void setUp() throws Exception {

        configuration = new Configurations().properties(getResource("cryptotrader-test.properties"));

        target = spy(new BitmexContext());

        target.setConfiguration(configuration);

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

        Key key = Key.builder().instrument("XBT_QT").timestamp(Instant.now()).build();

        // Tick
        System.out.println("Ask : " + target.getBestAskPrice(key));
        System.out.println("Bid : " + target.getBestBidPrice(key));
        System.out.println("Mid : " + target.getMidPrice(key));
        System.out.println("Ltp : " + target.getLastPrice(key));

        // Book
        System.out.println("ASZ : " + target.getBestAskSize(key));
        System.out.println("BSZ : " + target.getBestBidSize(key));

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
                case ETHXBT:
                case ETHXBT30M:
                case ETCXBT:
                case ETCXBT30M:
                case BCHXBT:
                case BCHXBT30M:
                    expect = "." + product.name();
                    break;
                case XBT:
                    expect = "XBT";
                    break;
                case XBTUSD:
                case XBT_FR:
                    expect = "XBTUSD";
                    break;
                case XBT_QT:
                    expect = "XBTZ17";
                    break;
                case XBJ_QT:
                    expect = "XBJZ17";
                    break;
                case ETH_QT:
                    expect = "ETHZ17";
                    break;
                case ETC_WK:
                    expect = "ETC7D";
                    break;
                case BCH_MT:
                    expect = "BCHX17";
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
        Key key4 = Key.builder().instrument("XBT_FR").build();
        doReturn("XBTUSD").when(target).convertAlias(key1);
        doReturn(".BXBT").when(target).convertAlias(key2);
        doReturn("XBTZ17").when(target).convertAlias(key3);
        doReturn("XBTUSD").when(target).convertAlias(key4);

        Optional<BitmexTick> result = target.queryTick(key1);
        assertTrue(result.isPresent());
        assertEquals(result.get().getSymbol(), "XBTUSD");
        assertEquals(result.get().getSettleCurrency(), "XBt");
        assertEquals(result.get().getState(), "Open");
        assertEquals(result.get().getTimestamp(), Instant.parse("2017-11-01T22:13:32.101Z"));
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
        assertEquals(result.get().getFundingFee(), new BigDecimal("-0.001213"));

        result = target.queryTick(key2);
        assertTrue(result.isPresent());
        assertEquals(result.get().getSymbol(), ".BXBT");
        assertEquals(result.get().getSettleCurrency(), "");
        assertEquals(result.get().getState(), "Unlisted");
        assertEquals(result.get().getTimestamp(), Instant.parse("2017-11-01T22:13:20.000Z"));
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
        assertEquals(result.get().getFundingFee(), null);

        result = target.queryTick(key3);
        assertTrue(result.isPresent());
        assertEquals(result.get().getSymbol(), "XBTZ17");
        assertEquals(result.get().getSettleCurrency(), "XBt");
        assertEquals(result.get().getState(), "Open");
        assertEquals(result.get().getTimestamp(), Instant.parse("2017-11-03T07:28:24.468Z"));
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
        assertEquals(result.get().getFundingFee(), null);

        result = target.queryTick(key4);
        assertTrue(result.isPresent());
        assertEquals(result.get().getSymbol(), "XBT_FR");
        assertEquals(result.get().getSettleCurrency(), null);
        assertEquals(result.get().getState(), "Unlisted");
        assertEquals(result.get().getTimestamp(), Instant.parse("2017-11-01T22:13:32.101Z"));
        assertEquals(result.get().getLast(), new BigDecimal("0.998787"));
        assertEquals(result.get().getAsk(), null);
        assertEquals(result.get().getBid(), null);
        assertEquals(result.get().getMid(), null);
        assertEquals(result.get().getLotSize(), null);
        assertEquals(result.get().getTickSize(), null);
        assertEquals(result.get().getExpiry(), null);
        assertEquals(result.get().getReference(), null);
        assertEquals(result.get().getMakerFee(), null);
        assertEquals(result.get().getTakerFee(), null);
        assertEquals(result.get().getSettleFee(), null);
        assertEquals(result.get().getFundingFee(), new BigDecimal("-0.001213"));

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
    public void testQueryBooks() throws Exception {

        doReturn(Resources.toString(getResource("json/bitmex_book.json"), UTF_8)).when(target)
                .request(GET, "https://www.bitmex.com/api/v1/orderBook/L2?symbol=XBTUSD&depth=1", null, null);

        Key key = Key.builder().instrument("XBT???").build();
        doReturn("XBTUSD").when(target).convertAlias(key);

        List<BitmexBook> result = target.queryBooks(key);
        assertEquals(result.size(), 4);
        assertEquals(result.get(0).getSide(), "Sell");
        assertEquals(result.get(0).getPrice(), new BigDecimal("6573.5"));
        assertEquals(result.get(0).getSize(), new BigDecimal("137"));
        assertEquals(result.get(1).getSide(), "Sell");
        assertEquals(result.get(1).getPrice(), new BigDecimal("6573"));
        assertEquals(result.get(1).getSize(), new BigDecimal("65348"));
        assertEquals(result.get(2).getSide(), "Buy");
        assertEquals(result.get(2).getPrice(), new BigDecimal("6572.5"));
        assertEquals(result.get(2).getSize(), new BigDecimal("22050"));
        assertEquals(result.get(3).getSide(), "Buy");
        assertEquals(result.get(3).getPrice(), new BigDecimal("6571.5"));
        assertEquals(result.get(3).getSize(), new BigDecimal("797"));

        // Empty
        target.clear();
        doReturn(null).when(target).request(any(), any(), any(), any());
        assertEquals(target.queryBooks(key).size(), 0);

        // Exception
        target.clear();
        doThrow(new IOException("test")).when(target).request(any(), any(), any(), any());
        assertEquals(target.queryBooks(key).size(), 0);

    }

    @Test
    public void testGetBestAskPrice() throws Exception {

        Key key = Key.builder().build();

        // Listed
        doReturn(of(BitmexTick.builder().ask(TEN).last(ONE).build())).when(target).queryTick(key);
        assertEquals(target.getBestAskPrice(key), TEN);

        // Unlisted
        doReturn(of(BitmexTick.builder().ask(TEN).last(ONE).state("Unlisted").build())).when(target).queryTick(key);
        assertEquals(target.getBestAskPrice(key), ONE);

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertEquals(target.getBestAskPrice(key), null);

    }

    @Test
    public void testGetBestBidPrice() throws Exception {

        Key key = Key.builder().build();

        // Listed
        doReturn(of(BitmexTick.builder().bid(TEN).last(ONE).build())).when(target).queryTick(key);
        assertEquals(target.getBestBidPrice(key), TEN);

        // Unlisted
        doReturn(of(BitmexTick.builder().bid(TEN).last(ONE).state("Unlisted").build())).when(target).queryTick(key);
        assertEquals(target.getBestBidPrice(key), ONE);

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertEquals(target.getBestBidPrice(key), null);

    }

    @Test
    public void testGetBestBboSize() throws Exception {

        Key key = Key.builder().build();

        List<BitmexBook> books = new ArrayList<>();
        books.add(BitmexBook.builder().side("Sell").price(valueOf(9)).size(valueOf(19)).build());
        books.add(BitmexBook.builder().side("Sell").price(null).size(valueOf(18)).build());
        books.add(BitmexBook.builder().side("Sell").price(valueOf(7)).size(valueOf(17)).build());
        books.add(BitmexBook.builder().side("Sell").price(valueOf(6)).size(null).build());
        books.add(BitmexBook.builder().side(null).price(valueOf(5)).size(valueOf(15)).build());
        books.add(BitmexBook.builder().side("Buy").price(null).size(valueOf(26)).build());
        books.add(BitmexBook.builder().side("Buy").price(valueOf(3)).size(valueOf(27)).build());
        books.add(BitmexBook.builder().side("Buy").price(valueOf(2)).size(null).build());
        books.add(BitmexBook.builder().side("Buy").price(valueOf(1)).size(valueOf(29)).build());
        books.add(null);
        doReturn(books).when(target).queryBooks(key);

        // Plain
        doReturn(Optional.empty()).when(target).queryTick(key);
        assertEquals(target.getBestAskSize(key), valueOf(17));
        assertEquals(target.getBestBidSize(key), valueOf(27));

        // Unlisted
        BitmexTick tick = mock(BitmexTick.class);
        when(tick.getState()).thenReturn("Unlisted");
        doReturn(Optional.of(tick)).when(target).queryTick(key);
        assertEquals(target.getBestAskSize(key), ZERO);
        assertEquals(target.getBestBidSize(key), ZERO);

        // No data
        doReturn(Optional.empty()).when(target).queryTick(key);
        doReturn(Collections.emptyList()).when(target).queryBooks(key);
        assertEquals(target.getBestAskSize(key), null);
        assertEquals(target.getBestBidSize(key), null);

    }

    @Test
    public void testGetMidPrice() throws Exception {

        Key key = Key.builder().build();

        // Listed
        doReturn(of(BitmexTick.builder().mid(TEN).last(ONE).build())).when(target).queryTick(key);
        assertEquals(target.getMidPrice(key), TEN);

        // Unlisted
        doReturn(of(BitmexTick.builder().mid(TEN).last(ONE).state("Unlisted").build())).when(target).queryTick(key);
        assertEquals(target.getMidPrice(key), ONE);

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
                "https://www.bitmex.com/api/v1/trade?count=500&reverse=true&symbol=XBTZ17",
                null, null);
        doReturn(Resources.toString(getResource("json/bitmex_bucket.json"), UTF_8))
                .when(target).request(GET,
                "https://www.bitmex.com/api/v1/trade/bucketed?binSize=1m&partial=true&count=500&reverse=true&symbol=XBJZ17",
                null, null);

        Key key = Key.builder().instrument("XBT_QT").timestamp(Instant.parse("2017-11-01T23:15:48.000Z")).build();
        BitmexTick tick = spy(BitmexTick.builder().timestamp(Instant.now()).last(TEN).build());
        doReturn(Optional.of(tick)).when(target).queryTick(key);
        doReturn("XBTZ17").when(target).convertAlias(key);

        // Unlisted (Index)
        when(tick.getState()).thenReturn("Unlisted");
        List<Trade> trades = target.listTrades(key, null);
        assertEquals(trades.size(), 1);
        assertEquals(trades.get(0).getPrice(), tick.getLast());
        assertEquals(trades.get(0).getSize(), ZERO);
        assertEquals(trades.get(0).getTimestamp(), tick.getTimestamp());
        assertEquals(trades.get(0).getBuyOrderId(), null);
        assertEquals(trades.get(0).getSellOrderId(), null);

        // Listed
        when(tick.getState()).thenReturn(null);
        trades = target.listTrades(key, null);
        assertEquals(trades.size(), 2);
        assertEquals(trades.get(0).getPrice(), new BigDecimal("6601.7"));
        assertEquals(trades.get(0).getSize(), new BigDecimal("686"));
        assertEquals(trades.get(0).getTimestamp(), Instant.parse("2017-11-01T22:15:47.303Z"));
        assertEquals(trades.get(0).getBuyOrderId(), "f391ff88-6731-f02d-46c2-b82471e762a9");
        assertEquals(trades.get(0).getSellOrderId(), "f391ff88-6731-f02d-46c2-b82471e762a9");
        assertEquals(trades.get(1).getPrice(), new BigDecimal("6601.6"));
        assertEquals(trades.get(1).getSize(), new BigDecimal("5"));
        assertEquals(trades.get(1).getTimestamp(), Instant.parse("2017-11-01T22:15:47.000Z"));
        assertEquals(trades.get(1).getBuyOrderId(), "bb1d629e-959c-298f-bab4-2921218193fa");
        assertEquals(trades.get(1).getSellOrderId(), "bb1d629e-959c-298f-bab4-2921218193fa");

        // Filtered in (all)
        trades = target.listTrades(key, Instant.parse("2017-11-01T22:15:47.000Z"));
        assertEquals(trades.size(), 2);

        // Filtered in
        trades = target.listTrades(key, Instant.parse("2017-11-01T22:15:47.303Z"));
        assertEquals(trades.size(), 1);
        assertEquals(trades.get(0).getTimestamp(), Instant.parse("2017-11-01T22:15:47.303Z"));

        // Filtered out
        trades = target.listTrades(key, Instant.parse("2017-11-01T22:15:47.304Z"));
        assertEquals(trades.size(), 0);

        // Bucketed
        key = Key.builder().instrument("XBJ_QT").timestamp(key.getTimestamp()).build();
        doReturn("XBJZ17").when(target).convertAlias(key);
        trades = target.listTrades(key, null);
        assertEquals(trades.size(), 2);
        assertEquals(trades.get(0).getPrice(), new BigDecimal("793399"));
        assertEquals(trades.get(0).getSize(), new BigDecimal("15000"));
        assertEquals(trades.get(0).getTimestamp(), Instant.parse("2017-11-05T06:50:00.000Z"));
        assertEquals(trades.get(0).getBuyOrderId(), null);
        assertEquals(trades.get(0).getSellOrderId(), null);
        assertEquals(trades.get(1).getPrice(), new BigDecimal("793399"));
        assertEquals(trades.get(1).getSize(), new BigDecimal("5000"));
        assertEquals(trades.get(1).getTimestamp(), Instant.parse("2017-11-05T06:48:00.000Z"));
        assertEquals(trades.get(1).getBuyOrderId(), null);
        assertEquals(trades.get(1).getSellOrderId(), null);

        // Filtered in (all)
        trades = target.listTrades(key, Instant.parse("2017-11-05T06:48:00.000Z"));
        assertEquals(trades.size(), 2);

        // Filtered in
        trades = target.listTrades(key, Instant.parse("2017-11-05T06:50:00.000Z"));
        assertEquals(trades.size(), 1);
        assertEquals(trades.get(0).getTimestamp(), Instant.parse("2017-11-05T06:50:00.000Z"));

        // Filtered out
        trades = target.listTrades(key, Instant.parse("2017-11-05T06:50:00.001Z"));
        assertEquals(trades.size(), 0);

        // No data
        key = Key.builder().instrument("ETH_QT").timestamp(key.getTimestamp()).build();
        trades = target.listTrades(key, null);
        assertEquals(trades.size(), 0);

        // Null key
        trades = target.listTrades(null, null);
        assertEquals(trades.size(), 0);

    }

    @Test
    public void testGetInstrumentCurrency() {

        Key key = Key.builder().build();
        assertEquals(target.getInstrumentCurrency(key), null);
        assertEquals(target.getInstrumentCurrency(null), null);

        key = Key.builder().instrument("XBTUSD").build();
        assertEquals(target.getInstrumentCurrency(key), BTC);

        key = Key.builder().instrument("XBJ_QT").build();
        assertEquals(target.getInstrumentCurrency(key), BTC);

        key = Key.builder().instrument("ETH_QT").build();
        assertEquals(target.getInstrumentCurrency(key), ETH);

    }

    @Test
    public void testGetFundingCurrency() {

        Key key = Key.builder().build();
        assertEquals(target.getFundingCurrency(key), null);
        assertEquals(target.getFundingCurrency(null), null);

        key = Key.builder().instrument("XBTUSD").build();
        assertEquals(target.getFundingCurrency(key), USD);

        key = Key.builder().instrument("XBJ_QT").build();
        assertEquals(target.getFundingCurrency(key), JPY);

        key = Key.builder().instrument("ETH_QT").build();
        assertEquals(target.getFundingCurrency(key), BTC);

    }

    @Test
    public void testGetConversionPrice() {

        Key key1 = Key.builder().instrument("XBTUSD").build();
        Key key2 = Key.builder().instrument("XBJ_QT").build();
        Key key3 = Key.builder().instrument("ETH_QT").build();
        Key key4 = Key.builder().instrument("FOOBAR").build();

        doReturn(null).when(target).getMidPrice(any());
        doReturn(new BigDecimal("6543")).when(target).getMidPrice(key1);
        doReturn(new BigDecimal("765432")).when(target).getMidPrice(key2);
        doReturn(new BigDecimal("0.0432")).when(target).getMidPrice(key3);

        // Structure
        assertEquals(target.getConversionPrice(key1, BTC), new BigDecimal("6543.0000000000"));
        assertEquals(target.getConversionPrice(key2, BTC), new BigDecimal("7654.3200000000"));
        assertEquals(target.getConversionPrice(key3, BTC), new BigDecimal("0023.1481481481"));
        assertEquals(target.getConversionPrice(key4, BTC), null);

        // Funding
        assertEquals(target.getConversionPrice(key1, USD), new BigDecimal("0.0001528351"));
        assertEquals(target.getConversionPrice(key2, JPY), new BigDecimal("0.0000000131"));
        assertEquals(target.getConversionPrice(key3, ETH), new BigDecimal("0.0432000000"));

        // Incompatible
        assertEquals(target.getConversionPrice(key1, ETH), null);
        assertEquals(target.getConversionPrice(key2, USD), null);
        assertEquals(target.getConversionPrice(key3, JPY), null);

        // Invalid Price
        doReturn(null).when(target).getMidPrice(key1);
        doReturn(ZERO).when(target).getMidPrice(key2);
        doReturn(ZERO).when(target).getMidPrice(key3);
        assertEquals(target.getConversionPrice(key1, BTC), null);
        assertEquals(target.getConversionPrice(key2, BTC), null);
        assertEquals(target.getConversionPrice(key3, BTC), null);

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

        configuration.setProperty(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexContext.api.id",
                "my_id"
        );
        configuration.setProperty(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.bitmex.BitmexContext.api.secret",
                "my_secret"
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
        configuration.clear();
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
        doReturn(new BigDecimal("6500.12")).when(target).getFundingConversionRate(key1, XBT);
        doReturn(new BigDecimal("750000")).when(target).getFundingConversionRate(key2, XBT);

        // Found (margin = 49990819 SATOSHI)
        assertEquals(target.getFundingPosition(key1), new BigDecimal("3246.7830945044"));
        assertEquals(target.getFundingPosition(key2), new BigDecimal("374621.90250000"));
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
    public void testGetFundingConversionRate() {

        Key key = Key.builder().instrument(ProductType.XBJ_QT.name()).build();

        // Null Price
        doReturn(null).when(target).getMidPrice(key);
        assertNull(target.getFundingConversionRate(key, XBT));

        // Zero Price
        doReturn(new BigDecimal("0.0")).when(target).getMidPrice(key);
        assertNull(target.getFundingConversionRate(key, XBT));

        // Valid
        doReturn(new BigDecimal("795000")).when(target).getMidPrice(key);
        assertEquals(target.getFundingConversionRate(key, XBT), new BigDecimal("6320250000.0000000000"));

        // Null Arguments
        assertNull(target.getFundingConversionRate(key, null));
        assertNull(target.getFundingConversionRate(null, XBT));

        // Unknown Product
        key = Key.builder().instrument("foo").build();
        assertNull(target.getFundingConversionRate(key, XBT));

        // Not traded
        key = Key.builder().instrument(ProductType.BXBTJPY.name()).build();
        assertNull(target.getFundingConversionRate(key, XBT));

        // Funding Instrument
        key = Key.builder().instrument(ProductType.ETH_QT.name()).build();
        assertEquals(target.getFundingConversionRate(key, XBT), new BigDecimal("1.0000000000"));

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

        // With negative commission
        doReturn(of(BitmexTick.builder().makerFee(ONE.negate()).settleFee(TEN).build())).when(target).queryTick(key);
        assertEquals(target.getCommissionRate(key), TEN);

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
