package com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck;

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

import static com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck.CoincheckContext.URL_TICKER;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.coincheck.CoincheckContext.URL_TRADE;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType.GET;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class CoincheckContextTest {

    private CoincheckContext target;

    @BeforeMethod
    public void setUp() throws Exception {

        target = spy(new CoincheckContext());

        doReturn(null).when(target).request(any(), any(), any(), any());

    }

    @AfterMethod
    public void tearDown() throws Exception {
        target.close();
    }

    @Test(enabled = false)
    public void test() throws IOException {

        doCallRealMethod().when(target).request(any(), any(), any(), any());

        Key key = Key.builder().instrument("btc_jpy").build();

        System.out.println("ASK : " + target.getBestAskPrice(key));
        System.out.println("BID : " + target.getBestBidPrice(key));
        System.out.println("MID : " + target.getMidPrice(key));
        System.out.println("LTP : " + target.getLastPrice(key));
        System.out.println("TRD : " + target.listTrades(key, null));

    }

    @Test
    public void testGet() {
        assertEquals(target.get(), "coincheck");
    }

    @Test
    public void testQueryTick() throws Exception {

        String data = Resources.toString(getResource("json/coincheck_ticker.json"), UTF_8);
        doReturn(data).when(target).request(GET, URL_TICKER, null, null);

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
    public void testGetBestAskPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        CoincheckTick tick = mock(CoincheckTick.class);
        when(tick.getAsk()).thenReturn(BigDecimal.TEN);

        doReturn(Optional.of(tick)).when(target).queryTick(key);
        assertEquals(target.getBestAskPrice(key), tick.getAsk());

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertNull(target.getBestAskPrice(key));

    }

    @Test
    public void testGetBestBidPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        CoincheckTick tick = mock(CoincheckTick.class);
        when(tick.getBid()).thenReturn(BigDecimal.TEN);

        doReturn(Optional.of(tick)).when(target).queryTick(key);
        assertEquals(target.getBestBidPrice(key), tick.getBid());

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertNull(target.getBestBidPrice(key));

    }

    @Test
    public void testGetLastPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        CoincheckTick tick = mock(CoincheckTick.class);
        when(tick.getLast()).thenReturn(BigDecimal.TEN);

        doReturn(Optional.of(tick)).when(target).queryTick(key);
        assertEquals(target.getLastPrice(key), tick.getLast());

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertNull(target.getLastPrice(key));

    }

    @Test
    public void testListTrades() throws Exception {

        Key key = Key.builder().instrument("btc_jpy").build();
        String data = Resources.toString(getResource("json/coincheck_trade.json"), UTF_8);
        doReturn(data).when(target).request(GET, URL_TRADE, null, null);

        // Found
        List<Trade> values = target.listTrades(key, null);
        assertEquals(values.size(), 2);
        assertEquals(values.get(0).getTimestamp(), Instant.ofEpochMilli(1505490550000L));
        assertEquals(values.get(0).getPrice(), new BigDecimal("396373"));
        assertEquals(values.get(0).getSize(), new BigDecimal("0.1"));
        assertEquals(values.get(0).getBuyOrderId(), null);
        assertEquals(values.get(0).getSellOrderId(), null);
        assertEquals(values.get(1).getTimestamp(), Instant.ofEpochMilli(1505490548000L));
        assertEquals(values.get(1).getPrice(), new BigDecimal("396377"));
        assertEquals(values.get(1).getSize(), new BigDecimal("0.0286585"));
        assertEquals(values.get(1).getBuyOrderId(), null);
        assertEquals(values.get(1).getSellOrderId(), null);

        // Not found
        List<Trade> unknown = target.listTrades(Key.builder().instrument("FOO").build(), null);
        assertEquals(unknown.size(), 0);

        // Cached
        doReturn(null).when(target).request(any(), any(), any(), any());
        List<Trade> cached = target.listTrades(key, null);
        assertEquals(cached, values);

        // Filtered
        List<Trade> filtered = target.listTrades(key, Instant.ofEpochMilli(1505490549000L));
        assertEquals(filtered.size(), 1);
        assertEquals(filtered.get(0), values.get(0));

    }

}
