package com.after_sunrise.cryptocurrency.cryptotrader.service.oanda;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.google.common.io.Resources;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType.GET;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class OandaContextTest {

    private OandaContext target;

    private Configuration configuration;

    @BeforeMethod
    public void setUp() throws Exception {

        configuration = new Configurations().properties(getResource("cryptotrader-test.properties"));

        target = spy(new OandaContext());

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

        Key key = Key.builder().instrument("USD_JPY").build();

        System.out.println("ASK : " + target.getBestAskPrice(key));
        System.out.println("BID : " + target.getBestBidPrice(key));

    }

    @Test
    public void testGet() {
        assertEquals(target.get(), "oanda");
    }

    @Test
    public void testQueryTick() throws Exception {

        Key.KeyBuilder builder = Key.builder().instrument("USD_JPY");

        String url = OandaContext.URL_TICKER + "USD_JPY";
        String data = Resources.toString(getResource("json/oanda_ticker.json"), UTF_8);
        String token = "my-token";
        Map<String, String> params = singletonMap("Authorization", "Bearer " + token);
        doReturn(data).when(target).request(GET, url, params, null);
        configuration.setProperty(
                "com.after_sunrise.cryptocurrency.cryptotrader.service.oanda.OandaContext.api.secret"
                , token);

        // Found
        OandaTick tick = target.queryTick(builder.build()).get();
        assertEquals(tick.getAsk(), new BigDecimal("110.859"));
        assertEquals(tick.getBid(), new BigDecimal("110.819"));
        verify(target, times(1)).request(GET, url, params, null);

        // Cached
        tick = target.queryTick(builder.build()).get();
        assertEquals(tick.getAsk(), new BigDecimal("110.859"));
        assertEquals(tick.getBid(), new BigDecimal("110.819"));
        verify(target, times(1)).request(any(), any(), any(), any());

        // Halted
        assertFalse(target.queryTick(builder.instrument("FOO_BAR").build()).isPresent());
        verify(target, times(2)).request(any(), any(), any(), any());

        // Not found
        assertFalse(target.queryTick(builder.instrument("FOO").build()).isPresent());
        verify(target, times(3)).request(any(), any(), any(), any());

        // No token
        configuration.clear();
        verify(target, times(3)).request(any(), any(), any(), any());

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
