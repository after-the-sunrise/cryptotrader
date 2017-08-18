package com.after_sunrise.cryptocurrency.cryptotrader.service.bitflyer;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitflyerAgentTest {

    private BitflyerAgent target;

    @BeforeMethod
    public void setUp() throws Exception {
        target = new BitflyerAgent();
    }

    @Test
    public void testGet() {
        assertEquals(target.get(), BitflyerService.ID);
    }

}
