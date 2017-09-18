package com.after_sunrise.cryptocurrency.cryptotrader.service.oanda;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.google.common.io.Resources;
import org.apache.http.impl.client.CloseableHttpClient;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class OandaContextTest {

    private OandaContext target;

    private CloseableHttpClient client;

    @BeforeMethod
    public void setUp() throws Exception {

        client = mock(CloseableHttpClient.class);

        target = spy(new OandaContext());

        doReturn(null).when(target).query(anyString(), any());

    }

    @Test(enabled = false)
    public void test() throws IOException {

        doCallRealMethod().when(target).query(any(), any());

        Key key = Key.builder().instrument("USD_JPY").timestamp(Instant.now()).build();

        System.out.println(target.queryTick(key));

    }

    @Test
    public void testGet() {
        assertEquals(target.get(), "oanda");
    }

    @Test
    public void testGetToken() {

        Path path1 = Paths.get("src", "test", "resources", ".oandajp");
        String token = target.getToken(path1.toAbsolutePath().toString(), "OJP_TOKEN");
        assertEquals(token, "MY_REST_TOKEN_HERE");

        Path path2 = Paths.get("src", "test", "resources", ".unknown");
        assertNull(target.getToken(path2.toAbsolutePath().toString(), "OJP_TOKEN"));

    }

    @Test
    public void testQueryTick() throws Exception {

        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("GMT"));
        Instant now = ZonedDateTime.parse("2017-09-15T21:00:00Z", dtf).toInstant();
        Key.KeyBuilder builder = Key.builder().instrument("USD_JPY").timestamp(now);

        String data = Resources.toString(getResource("json/oanda_ticker.json"), UTF_8);
        Map<String, String> params = singletonMap("Authorization", "Bearer my-token");
        doReturn(data).when(target).query(OandaContext.URL_TICKER + "USD_JPY", params);
        doReturn("my-token").when(target).getToken(anyString(), anyString());

        // Found
        OandaTick tick = target.queryTick(builder.build()).get();
        assertEquals(tick.getAsk(), new BigDecimal("110.859"));
        assertEquals(tick.getBid(), new BigDecimal("110.819"));
        verify(target, times(1)).query(any(), any());

        // Cached
        tick = target.queryTick(builder.build()).get();
        assertEquals(tick.getAsk(), new BigDecimal("110.859"));
        assertEquals(tick.getBid(), new BigDecimal("110.819"));
        verify(target, times(1)).query(any(), any());

        // Stale
        assertFalse(target.queryTick(builder.timestamp(now.plus(1, HOURS)).build()).isPresent());
        verify(target, times(2)).query(any(), any());

        // Not found
        assertFalse(target.queryTick(builder.timestamp(now).instrument("FOO").build()).isPresent());
        verify(target, times(3)).query(any(), any());

        // No token
        doReturn(null).when(target).getToken(any(), any());
        assertFalse(target.queryTick(builder.timestamp(now).instrument("BAR").build()).isPresent());
        verify(target, times(3)).query(any(), any());

    }

    @Test
    public void testGetBestAskPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        OandaTick tick = mock(OandaTick.class);
        when(tick.getAsk()).thenReturn(BigDecimal.TEN);

        doReturn(Optional.of(tick)).when(target).queryTick(key);
        assertEquals(target.getBestAskPrice(key), tick.getAsk());

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertNull(target.getBestAskPrice(key));

    }

    @Test
    public void testGetBestBidPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        OandaTick tick = mock(OandaTick.class);
        when(tick.getBid()).thenReturn(BigDecimal.TEN);

        doReturn(Optional.of(tick)).when(target).queryTick(key);
        assertEquals(target.getBestBidPrice(key), tick.getBid());

        doReturn(Optional.empty()).when(target).queryTick(key);
        assertNull(target.getBestBidPrice(key));

    }

}
