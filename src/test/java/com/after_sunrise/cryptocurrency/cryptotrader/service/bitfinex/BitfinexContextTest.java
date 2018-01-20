package com.after_sunrise.cryptocurrency.cryptotrader.service.bitfinex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.google.common.io.Resources;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.*;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitfinex.BitfinexContext.URL_TICKER;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitfinex.BitfinexContext.URL_TRADE;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType.GET;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitfinexContextTest {

    private BitfinexContext target;

    @BeforeMethod
    public void setUp() throws Exception {

        target = spy(new BitfinexContext());

        doReturn(null).when(target).request(any(), any(), any(), any());

    }

    @AfterMethod
    public void tearDown() throws Exception {
        target.close();
    }

    @Test(enabled = false)
    public void test() throws IOException {

        doCallRealMethod().when(target).request(any(), any(), any(), any());

        Key key = Key.builder().instrument("ethbtc").build();

        System.out.println("ASK : " + target.getBestAskPrice(key));
        System.out.println("BID : " + target.getBestBidPrice(key));
        System.out.println("MID : " + target.getMidPrice(key));
        System.out.println("LTP : " + target.getLastPrice(key));
        System.out.println("TRD : " + target.listTrades(key, null));

    }

    @Test
    public void testGet() {
        assertEquals(target.get(), "bitfinex");
    }

    @Test
    public void testQueryTick() throws Exception {

        String data = Resources.toString(getResource("json/bitfinex_ticker.json"), UTF_8);
        doReturn(data).when(target).request(GET, URL_TICKER + "ethbtc", null, null);

        // Found
        BitfinexTick tick = target.queryTick(Key.builder().instrument("ethbtc").build()).get();
        assertEquals(tick.getAsk(), new BigDecimal("0.11646"));
        assertEquals(tick.getBid(), new BigDecimal("0.1154"));
        assertEquals(tick.getLast(), new BigDecimal("0.11648"));

        // Not found
        assertFalse(target.queryTick(Key.builder().instrument("FOO_BAR").build()).isPresent());

        // Cached
        doReturn(null).when(target).request(any(), any(), any(), any());
        BitfinexTick cached = target.queryTick(Key.builder().instrument("ethbtc").build()).get();
        assertSame(cached, tick);

    }

    @Test
    public void testGetBestAskPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        BitfinexTick tick = mock(BitfinexTick.class);
        when(tick.getAsk()).thenReturn(BigDecimal.TEN);

        doReturn(Optional.of(tick)).when(target).queryTick(key);
        assertEquals(target.getBestAskPrice(key), tick.getAsk());

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertNull(target.getBestAskPrice(key));

    }

    @Test
    public void testGetBestBidPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        BitfinexTick tick = mock(BitfinexTick.class);
        when(tick.getBid()).thenReturn(BigDecimal.TEN);

        doReturn(Optional.of(tick)).when(target).queryTick(key);
        assertEquals(target.getBestBidPrice(key), tick.getBid());

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertNull(target.getBestBidPrice(key));

    }

    @Test
    public void testGetLastPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        BitfinexTick tick = mock(BitfinexTick.class);
        when(tick.getLast()).thenReturn(BigDecimal.TEN);

        doReturn(Optional.of(tick)).when(target).queryTick(key);
        assertEquals(target.getLastPrice(key), tick.getLast());

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertNull(target.getLastPrice(key));

    }

    @Test
    public void testListTrades() throws Exception {

        Key key = Key.builder().instrument("TEST").build();
        String data = Resources.toString(getResource("json/bitfinex_trade.json"), UTF_8);
        doReturn(data).when(target).request(GET, URL_TRADE + key.getInstrument(), null, null);

        // Found
        List<Trade> values = target.listTrades(key, null);
        assertEquals(values.size(), 2);
        assertEquals(values.get(0).getTimestamp(), Instant.ofEpochMilli(1505489261000L));
        assertEquals(values.get(0).getPrice(), new BigDecimal("0.11532"));
        assertEquals(values.get(0).getSize(), new BigDecimal("0.021299"));
        assertEquals(values.get(1).getTimestamp(), Instant.ofEpochMilli(1505489259000L));
        assertEquals(values.get(1).getPrice(), new BigDecimal("0.11593"));
        assertEquals(values.get(1).getSize(), new BigDecimal("2.9992"));

        // Cached
        doReturn(null).when(target).request(any(), any(), any(), any());
        List<Trade> cached = target.listTrades(key, null);
        assertEquals(cached, values);

        // Filtered
        List<Trade> filtered = target.listTrades(key, Instant.ofEpochMilli(1505489260000L));
        assertEquals(filtered.size(), 1);
        assertEquals(filtered.get(0), values.get(0));

    }

    @Test
    public void testFindProduct() {
        assertEquals(target.findProduct(null, BTC, USD), "btcusd");
        assertEquals(target.findProduct(null, BCH, BTC), "bchbtc");
        assertEquals(target.findProduct(null, ETH, BTC), "ethbtc");
        assertEquals(target.findProduct(null, JPY, BTC), null);
    }

}
