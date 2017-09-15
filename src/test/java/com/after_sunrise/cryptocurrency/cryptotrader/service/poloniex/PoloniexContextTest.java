package com.after_sunrise.cryptocurrency.cryptotrader.service.poloniex;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.google.common.io.Resources;
import org.apache.http.impl.client.CloseableHttpClient;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class PoloniexContextTest {

    private PoloniexContext target;

    private CloseableHttpClient client;

    @BeforeMethod
    public void setUp() throws Exception {

        client = mock(CloseableHttpClient.class);

        target = spy(new PoloniexContext());

        doReturn(null).when(target).query(anyString());

    }

    @Test
    public void testGet() {
        assertEquals(target.get(), "poloniex");
    }

    @Test
    public void testQueryTick() throws Exception {

        String data = Resources.toString(getResource("json/poloniex_ticker.json"), UTF_8);
        doReturn(data).when(target).query(PoloniexContext.URL_TICKER);

        // Found
        PoloniexTick tick = target.queryTick(Key.builder().instrument("BTC_ETH").build()).get();
        assertEquals(tick.getAsk(), new BigDecimal("0.07124943"));
        assertEquals(tick.getBid(), new BigDecimal("0.07116150"));
        assertEquals(tick.getLast(), new BigDecimal("0.07124943"));

        // Not found
        assertFalse(target.queryTick(Key.builder().instrument("FOO_BAR").build()).isPresent());

        // Cached
        doReturn(null).when(target).query(PoloniexContext.URL_TICKER);
        PoloniexTick cached = target.queryTick(Key.builder().instrument("BTC_ETH").build()).get();
        assertSame(cached, tick);

    }

    @Test
    public void testGetBestAskPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        PoloniexTick tick = mock(PoloniexTick.class);
        when(tick.getAsk()).thenReturn(BigDecimal.TEN);

        doReturn(Optional.of(tick)).when(target).queryTick(key);
        assertEquals(target.getBestAskPrice(key), tick.getAsk());

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertNull(target.getBestAskPrice(key));

    }

    @Test
    public void testGetBestBidPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        PoloniexTick tick = mock(PoloniexTick.class);
        when(tick.getBid()).thenReturn(BigDecimal.TEN);

        doReturn(Optional.of(tick)).when(target).queryTick(key);
        assertEquals(target.getBestBidPrice(key), tick.getBid());

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertNull(target.getBestBidPrice(key));

    }

    @Test
    public void testGetMidPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        doReturn(null).when(target).getBestAskPrice(key);
        doReturn(null).when(target).getBestBidPrice(key);
        assertNull(target.getMidPrice(key));

        doReturn(BigDecimal.TEN).when(target).getBestAskPrice(key);
        doReturn(null).when(target).getBestBidPrice(key);
        assertNull(target.getMidPrice(key));

        doReturn(null).when(target).getBestAskPrice(key);
        doReturn(BigDecimal.ONE).when(target).getBestBidPrice(key);
        assertNull(target.getMidPrice(key));

        doReturn(BigDecimal.TEN).when(target).getBestAskPrice(key);
        doReturn(BigDecimal.ONE).when(target).getBestBidPrice(key);
        assertEquals(target.getMidPrice(key), new BigDecimal("5.5"));

    }

    @Test
    public void testGetLastPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        PoloniexTick tick = mock(PoloniexTick.class);
        when(tick.getLast()).thenReturn(BigDecimal.TEN);

        doReturn(Optional.of(tick)).when(target).queryTick(key);
        assertEquals(target.getLastPrice(key), tick.getLast());

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertNull(target.getLastPrice(key));

    }


    @Test
    public void testListTrades() throws Exception {

        Key key = Key.builder().instrument("TEST").build();
        String data = Resources.toString(getResource("json/poloniex_trade.json"), UTF_8);
        doReturn(data).when(target).query(PoloniexContext.URL_TRADE + key.getInstrument());

        // Found
        List<Trade> values = target.listTrades(key, null);
        assertEquals(values.size(), 2);
        assertEquals(values.get(0).getTimestamp(), Instant.ofEpochMilli(1505230796000L));
        assertEquals(values.get(0).getPrice(), new BigDecimal("0.07140271"));
        assertEquals(values.get(0).getSize(), new BigDecimal("0.20000000"));
        assertEquals(values.get(0).getBuyOrderId(), null);
        assertEquals(values.get(0).getSellOrderId(), null);
        assertEquals(values.get(1).getTimestamp(), Instant.ofEpochMilli(1505230180000L));
        assertEquals(values.get(1).getPrice(), new BigDecimal("0.07124940"));
        assertEquals(values.get(1).getSize(), new BigDecimal("0.10772398"));
        assertEquals(values.get(1).getBuyOrderId(), null);
        assertEquals(values.get(1).getSellOrderId(), null);

        // Cached
        doReturn(null).when(target).query(PoloniexContext.URL_TRADE);
        List<Trade> cached = target.listTrades(key, null);
        assertEquals(cached, values);

        // Filtered
        List<Trade> filtered = target.listTrades(key, Instant.ofEpochMilli(1505230700000L));
        assertEquals(filtered.size(), 1);
        assertEquals(filtered.get(0), values.get(0));

    }

}
