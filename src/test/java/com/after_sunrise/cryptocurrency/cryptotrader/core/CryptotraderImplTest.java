package com.after_sunrise.cryptocurrency.cryptotrader.core;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class CryptotraderImplTest {

    static class TestTrader implements Trader {

        static final AtomicInteger COUNT = new AtomicInteger(0);

        @Override
        public void trade() {
            COUNT.incrementAndGet();
        }

        @Override
        public void trigger() {
            COUNT.incrementAndGet();
        }

        @Override
        public void close() {
            COUNT.decrementAndGet();
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

            assertEquals(TestTrader.COUNT.get(), 0);

            target.execute();

            target.execute();

            assertEquals(TestTrader.COUNT.get(), 1);

        } finally {

            target.shutdown();

            target.shutdown();

            assertEquals(TestTrader.COUNT.get(), 0);

        }

    }

}
