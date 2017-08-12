package com.after_sunrise.cryptocurrency.cryptotrader.core;

import org.apache.commons.configuration2.Configuration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.after_sunrise.cryptocurrency.cryptotrader.core.ConfigurationSupplier.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class ConfigurationSupplierTest {

    private static final String TEST = "cryptotrader-test.properties";

    private static final String KEY = "cryptotrader.version";

    private ConfigurationSupplier target;

    @BeforeMethod
    public void setUp() {
        target = spy(new ConfigurationSupplier());
    }

    @Test
    public void testGet() throws Exception {

        assertNotNull(target.get());

        verify(target).get(VERSION, SITE, DEFAULT);

    }

    @Test
    public void testGet_WithArgs() throws Exception {

        // Plain
        Configuration c = target.get(VERSION, SITE, DEFAULT);
        assertNotNull(c.getString(KEY));
        assertNotEquals(c.getString(KEY), "default");
        assertNotEquals(c.getString(KEY), "test");

        // Version = test
        c = target.get(TEST, SITE, DEFAULT);
        assertEquals(c.getString(KEY), "test");

        // Site = test (found)
        c = target.get(VERSION, "src/test/resources/" + TEST, DEFAULT);
        assertNotNull(c.getString(KEY));
        assertNotEquals(c.getString(KEY), "default");
        assertNotEquals(c.getString(KEY), "test");

        // Site = test (not found)
        c = target.get(VERSION, "/dev/null/" + TEST, DEFAULT);
        assertNotNull(c.getString(KEY));
        assertNotEquals(c.getString(KEY), "default");
        assertNotEquals(c.getString(KEY), "test");

        // Default = test
        c = target.get(VERSION, SITE, TEST);
        assertNotNull(c.getString(KEY));
        assertNotEquals(c.getString(KEY), "default");
        assertNotEquals(c.getString(KEY), "test");

        // Invalid path
        try {
            target.get("/tmp/" + TEST, SITE, DEFAULT);
            fail();
        } catch (RuntimeException e) {
            // Success
        }

    }

}
