package com.after_sunrise.cryptocurrency.cryptotrader.service.fisco;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
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
import static com.after_sunrise.cryptocurrency.cryptotrader.service.fisco.FiscoContext.URL_TRADE;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.fisco.FiscoService.ID;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.fisco.FiscoService.ProductType.BTC_JPY;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext.RequestType.GET;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class FiscoContextTest {

    private FiscoContext target;

    @BeforeMethod
    public void setUp() throws Exception {

        target = spy(new FiscoContext());

        doReturn(null).when(target).request(any(), any(), any(), any());

    }

    @AfterMethod
    public void tearDown() throws Exception {
        target.close();
    }

    @Test(enabled = false)
    public void test() throws ConfigurationException, IOException {

        doCallRealMethod().when(target).request(any(), any(), any(), any());

        Path path = Paths.get(System.getProperty("user.home"), ".cryptotrader");
        target.setConfiguration(new Configurations().properties(path.toAbsolutePath().toFile()));
        Key key = Key.builder().site(ID).instrument(BTC_JPY.name()).timestamp(Instant.now()).build();

        System.out.println("AP: " + target.getBestAskPrice(key));
        System.out.println("BP: " + target.getBestBidPrice(key));
        System.out.println("AS: " + target.getBestAskSize(key));
        System.out.println("BS: " + target.getBestBidSize(key));
        System.out.println("MP: " + target.getMidPrice(key));
        System.out.println("AD: " + target.getAskPrices(key));
        System.out.println("BD: " + target.getBidPrices(key));

        System.out.println("LP:" + target.getLastPrice(key));
        System.out.println("TD:" + target.listTrades(key, null));

        System.out.println("IC:" + target.getInstrumentCurrency(key));
        System.out.println("FC:" + target.getFundingCurrency(key));
        System.out.println("FP:" + target.findProduct(key, BTC, JPY));
        System.out.println("CM:" + target.getCommissionRate(key));

        System.out.println("P1:" + target.getConversionPrice(key, BTC));
        System.out.println("P2:" + target.getConversionPrice(key, JPY));

        System.out.println("IP:" + target.getInstrumentPosition(key));
        System.out.println("FP:" + target.getFundingPosition(key));
        System.out.println("AO:" + target.listActiveOrders(key));
        System.out.println("EX:" + target.listExecutions(key));

    }

    @Test(enabled = false)
    public void testOrder() throws ConfigurationException, InterruptedException, IOException {

        doCallRealMethod().when(target).request(any(), any(), any(), any());

        Path path = Paths.get(System.getProperty("user.home"), ".cryptotrader");
        target.setConfiguration(new Configurations().properties(path.toAbsolutePath().toFile()));
        Key key = Key.builder().site(ID).instrument(BTC_JPY.name()).timestamp(Instant.now()).build();

        Set<Instruction.CreateInstruction> creates = Sets.newHashSet(
                Instruction.CreateInstruction.builder().price(new BigDecimal("700000")).size(new BigDecimal("+0.001")).build(),
                Instruction.CreateInstruction.builder().price(new BigDecimal("800000")).size(new BigDecimal("-0.001")).build()
        );

        Map<Instruction.CreateInstruction, String> created = target.createOrders(key, creates);
        System.out.println("Created : " + created);

        TimeUnit.SECONDS.sleep(5L);
        System.out.println("Orders:" + target.listActiveOrders(key));
        TimeUnit.SECONDS.sleep(30L);

        Set<Instruction.CancelInstruction> cancels = created.values().stream().filter(StringUtils::isNotEmpty)
                .map(id -> Instruction.CancelInstruction.builder().id(id).build()).collect(toSet());
        Map<Instruction.CancelInstruction, String> cancelled = target.cancelOrders(key, cancels);
        System.out.println("Cancelled : " + cancelled);

        TimeUnit.SECONDS.sleep(5L);
        System.out.println("Orders:" + target.listActiveOrders(key));

    }

    @Test
    public void testGet() {
        assertEquals(target.get(), "fisco");
    }

    @Test
    public void testGetAsk() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        FiscoDepth value = mock(FiscoDepth.class);
        when(value.getAskPrices()).thenReturn(new TreeMap<>());
        value.getAskPrices().put(BigDecimal.ONE, BigDecimal.TEN);

        doReturn(Optional.of(value)).when(target).queryDepth(key);
        assertEquals(target.getBestAskPrice(key), BigDecimal.ONE);
        assertEquals(target.getBestAskSize(key), BigDecimal.TEN);
        assertEquals(target.getAskPrices(key), value.getAskPrices());

        doReturn(Optional.empty()).when(target).queryDepth(key);
        assertNull(target.getBestAskPrice(key));
        assertNull(target.getBestAskSize(key));
        assertNull(target.getAskPrices(key));

    }

    @Test
    public void testGetBid() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        FiscoDepth value = mock(FiscoDepth.class);
        when(value.getBidPrices()).thenReturn(new TreeMap<>());
        value.getBidPrices().put(BigDecimal.ONE, BigDecimal.TEN);

        doReturn(Optional.of(value)).when(target).queryDepth(key);
        assertEquals(target.getBestBidPrice(key), BigDecimal.ONE);
        assertEquals(target.getBestBidSize(key), BigDecimal.TEN);

        doReturn(Optional.empty()).when(target).queryDepth(key);
        assertNull(target.getBestBidPrice(key));
        assertNull(target.getBestBidSize(key));
        assertNull(target.getBidPrices(key));

    }

    @Test
    public void testListTrades() throws Exception {

        Key key = Key.builder().instrument("BTC_JPY").build();
        String data = Resources.toString(getResource("json/fisco_trade.json"), UTF_8);
        doReturn(data).when(target).request(GET, URL_TRADE + "btc_jpy", null, null);

        // Found
        List<Trade> values = target.listTrades(key, null);
        assertEquals(values.size(), 2);
        assertEquals(values.get(0).getTimestamp(), Instant.ofEpochMilli(1505657008000L));
        assertEquals(values.get(0).getPrice(), new BigDecimal("407470"));
        assertEquals(values.get(0).getSize(), new BigDecimal("0.1871"));
        assertEquals(values.get(1).getTimestamp(), Instant.ofEpochMilli(1505657007000L));
        assertEquals(values.get(1).getPrice(), new BigDecimal("407505"));
        assertEquals(values.get(1).getSize(), new BigDecimal("0.0011"));

        // Not found
        List<Trade> unknown = target.listTrades(Key.builder().instrument("FOO").build(), null);
        assertEquals(unknown, null);

        // Cached
        doReturn(null).when(target).request(any(), any(), any(), any());
        List<Trade> cached = target.listTrades(key, null);
        assertEquals(cached, values);

        // Filtered
        List<Trade> filtered = target.listTrades(key, Instant.ofEpochMilli(1505657007500L));
        assertEquals(filtered.size(), 1);
        assertEquals(filtered.get(0), values.get(0));

    }

    @Test
    public void testFindProduct() {
        assertEquals(target.findProduct(null, BTC, JPY), "BTC_JPY");
        assertEquals(target.findProduct(null, BCH, JPY), "BCH_JPY");
        assertEquals(target.findProduct(null, BCH, BTC), "BCH_BTC");
    }

}
