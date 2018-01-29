package com.after_sunrise.cryptocurrency.cryptotrader.service.btcbox;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.BTC;
import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.JPY;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.btcbox.BtcboxService.ID;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.btcbox.BtcboxService.ProductType.BTC_JPY;
import static java.util.stream.Collectors.toSet;
import static org.mockito.Mockito.spy;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BtcboxContextTest {

    private BtcboxContext target;

    @BeforeMethod
    public void setUp() throws Exception {
        target = spy(new BtcboxContext());
    }

    @Test(enabled = false)
    public void test() throws ConfigurationException {

        Path path = Paths.get(System.getProperty("user.home"), ".cryptotrader");
        target.setConfiguration(new Configurations().properties(path.toAbsolutePath().toFile()));
        Context.Key key = Context.Key.builder().site(ID).instrument(BTC_JPY.name()).timestamp(Instant.now()).build();

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
        System.out.println("AO:" + target.listActiveOrders(key));
        System.out.println("EX:" + target.listExecutions(key));

    }

    @Test(enabled = false)
    public void testOrder() throws ConfigurationException, InterruptedException {

        Path path = Paths.get(System.getProperty("user.home"), ".cryptotrader");
        target.setConfiguration(new Configurations().properties(path.toAbsolutePath().toFile()));
        Context.Key key = Context.Key.builder().site(ID).instrument(BTC_JPY.name()).timestamp(Instant.now()).build();

        Set<Instruction.CreateInstruction> creates = Sets.newHashSet(
                Instruction.CreateInstruction.builder().price(new BigDecimal("1500000")).size(new BigDecimal("-0.001")).build(),
                Instruction.CreateInstruction.builder().price(new BigDecimal("1000000")).size(new BigDecimal("+0.001")).build()
        );

        Map<Instruction.CreateInstruction, String> created = target.createOrders(key, creates);
        System.out.println("Created : " + created);

        TimeUnit.SECONDS.sleep(5L);
        System.out.println("AO:" + target.listActiveOrders(key));

        TimeUnit.SECONDS.sleep(5L);
        created.values().stream().filter(StringUtils::isNotEmpty)
                .forEach(id -> System.out.println("FO:" + target.findOrder(key, id)));

        Set<Instruction.CancelInstruction> cancels = created.values().stream().filter(StringUtils::isNotEmpty)
                .map(id -> Instruction.CancelInstruction.builder().id(id).build()).collect(toSet());
        Map<Instruction.CancelInstruction, String> cancelled = target.cancelOrders(key, cancels);
        System.out.println("Cancelled : " + cancelled);

    }

}
