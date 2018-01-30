package com.after_sunrise.cryptocurrency.cryptotrader.service.bitbank;

import cc.bitbank.entity.enums.OrderSide;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order.Execution;
import com.google.common.collect.Sets;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.BTC;
import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.JPY;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitbank.BitbankService.ID;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitbank.BitbankService.ProductType.BTC_JPY;
import static java.math.BigDecimal.ZERO;
import static java.math.BigDecimal.valueOf;
import static java.util.stream.Collectors.toSet;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitbankContextTest {

    private BitbankContext target;

    @BeforeMethod
    public void setUp() throws Exception {
        target = spy(new BitbankContext());
    }

    @Test(enabled = false)
    public void test() throws ConfigurationException {

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

        System.out.println("P1:" + target.getConversionPrice(key, BTC));
        System.out.println("P2:" + target.getConversionPrice(key, JPY));

        System.out.println("IP:" + target.getInstrumentPosition(key));
        System.out.println("FP:" + target.getFundingPosition(key));

    }

    @Test(enabled = false)
    public void testOrder() throws ConfigurationException, InterruptedException {

        Path path = Paths.get(System.getProperty("user.home"), ".cryptotrader");
        target.setConfiguration(new Configurations().properties(path.toAbsolutePath().toFile()));
        Key key = Key.builder().site(ID).instrument(BTC_JPY.name()).timestamp(Instant.now()).build();

        Set<CreateInstruction> creates = Sets.newHashSet(
                CreateInstruction.builder().price(new BigDecimal("1000000")).size(new BigDecimal("+0.0001")).build(),
                CreateInstruction.builder().price(new BigDecimal("1500000")).size(new BigDecimal("-0.0001")).build()
        );

        Map<CreateInstruction, String> created = target.createOrders(key, creates);
        System.out.println("Created : " + created);

        TimeUnit.SECONDS.sleep(10L);

        Set<CancelInstruction> cancels = created.values().stream().filter(StringUtils::isNotEmpty)
                .map(id -> CancelInstruction.builder().id(id).build()).collect(toSet());
        Map<CancelInstruction, String> cancelled = target.cancelOrders(key, cancels);
        System.out.println("Cancelled : " + cancelled);

    }

    @Test
    public void testListExecutions() {

        Key key = Key.builder().site("s").instrument("i").timestamp(Instant.now()).build();

        doReturn(null).when(target).findOrder(any(), any());

        IntStream.range(0, 8).mapToObj(i -> {
            cc.bitbank.entity.Order o = new cc.bitbank.entity.Order();
            o.orderId = i;
            o.side = OrderSide.BUY;
            o.executedAmount = valueOf(i);
            return new BitbankOrder(o);
        }).forEach(o -> {
            target.getCachedOrders().put(o.getDelegate().orderId, o);
            doReturn(o).when(target).findOrder(key, o.getId());
        });

        // Finished
        target.getCachedOrders().get(0L).getDelegate().status = "CANCELED_UNFILLED";
        target.getCachedOrders().get(1L).getDelegate().status = "FULLY_FILLED";
        target.getCachedOrders().get(2L).getDelegate().status = "CANCELED_PARTIALLY_FILLED";
        target.getCachedOrders().get(7L).getDelegate().status = "CANCELED_PARTIALLY_FILLED";

        // Pending
        target.getCachedOrders().get(3L).getDelegate().status = null;
        target.getCachedOrders().get(4L).getDelegate().status = null;
        target.getCachedOrders().get(5L).getDelegate().status = "UNFILLED";
        target.getCachedOrders().get(6L).getDelegate().status = "UNFILLED";

        // No Fill
        target.getCachedOrders().get(3L).getDelegate().executedAmount = null;
        target.getCachedOrders().get(7L).getDelegate().executedAmount = ZERO;

        // Active orders are queried
        doReturn(null).when(target).findOrder(key, "5");

        // Skipped : id1 for zero qty, id3 for null qty.
        List<Execution> executions = target.listExecutions(key);
        assertEquals(executions.size(), 4);
        assertEquals(executions.get(0).getId(), "1");
        assertEquals(executions.get(1).getId(), "2");
        assertEquals(executions.get(2).getId(), "4");
        assertEquals(executions.get(3).getId(), "6");

        // Active orders are queried
        verify(target, times(4)).findOrder(any(), anyString());
        verify(target).findOrder(key, "3");
        verify(target).findOrder(key, "4");
        verify(target).findOrder(key, "5");
        verify(target).findOrder(key, "6");

        // Terminated orders without fills are cleared.
        assertEquals(target.getCachedOrders().size(), 5);
        assertTrue(target.getCachedOrders().containsKey(1L));
        assertTrue(target.getCachedOrders().containsKey(2L));
        assertTrue(target.getCachedOrders().containsKey(3L));
        assertTrue(target.getCachedOrders().containsKey(4L));
        assertTrue(target.getCachedOrders().containsKey(6L));

    }

}
