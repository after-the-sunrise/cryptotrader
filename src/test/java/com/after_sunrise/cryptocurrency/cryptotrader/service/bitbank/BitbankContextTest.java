package com.after_sunrise.cryptocurrency.cryptotrader.service.bitbank;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.BTC;
import static com.after_sunrise.cryptocurrency.cryptotrader.framework.Service.CurrencyType.JPY;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitbank.BitbankService.ID;
import static com.after_sunrise.cryptocurrency.cryptotrader.service.bitbank.BitbankService.ProductType.BTC_JPY;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitbankContextTest {

    private BitbankContext target;

    @BeforeMethod
    public void setUp() throws Exception {
        target = new BitbankContext();
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

}
