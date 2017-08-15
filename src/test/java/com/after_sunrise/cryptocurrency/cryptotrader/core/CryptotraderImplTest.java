package com.after_sunrise.cryptocurrency.cryptotrader.core;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader;
import org.testng.annotations.Test;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class CryptotraderImplTest {

    static class TestTrader implements Trader {
        @Override
        public void trade() {
        }
    }

    @Test
    public void testExecute_Dry() throws Exception {

        CryptotraderImpl target = new CryptotraderImpl();

        // Do not call "execute()"

        target.shutdown();

    }

    @Test
    public void testExecute_TestTrader() throws Exception {

        CryptotraderImpl target = new CryptotraderImpl(TestTrader.class);

        try {

            target.execute();

            target.execute();

        } finally {

            target.shutdown();

            target.shutdown();

        }

    }

}
