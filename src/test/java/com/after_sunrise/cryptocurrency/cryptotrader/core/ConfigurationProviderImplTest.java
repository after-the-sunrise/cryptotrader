package com.after_sunrise.cryptocurrency.cryptotrader.core;

import org.apache.commons.configuration2.Configuration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.after_sunrise.cryptocurrency.cryptotrader.core.ConfigurationProviderImpl.*;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class ConfigurationProviderImplTest {

    private static final String TEST = "cryptotrader-test.properties";

    private static final String KEY = "cryptotrader.version";

    private ConfigurationProviderImpl target;

    @BeforeMethod
    public void setUp() {
        target = spy(new ConfigurationProviderImpl());
    }

    @Test
    public void testGet() throws Exception {

        // Proxy (Invoke to initialize delegate)
        Configuration c = target.get();
        String version = c.getString(KEY);

        // Same proxy, same delegate.
        assertSame(target.get(), c);
        assertEquals(c.getString(KEY), version);

        // Same proxy, new delegate.
        target.clear();
        assertSame(target.get(), c);
        assertEquals(c.getString(KEY), version);

    }

    @Test
    public void testCreate() throws Exception {

        // Plain
        Configuration c = target.create(VERSION, SITE, DEFAULT);
        assertNotNull(c.getString(KEY));
        assertNotEquals(c.getString(KEY), "default");
        assertNotEquals(c.getString(KEY), "test");

        // Version = test
        c = target.create(TEST, SITE, DEFAULT);
        assertEquals(c.getString(KEY), "test");

        // Site = test (found)
        c = target.create(VERSION, "src/test/resources/" + TEST, DEFAULT);
        assertNotNull(c.getString(KEY));
        assertNotEquals(c.getString(KEY), "default");
        assertNotEquals(c.getString(KEY), "test");

        // Site = test (not found)
        c = target.create(VERSION, "/dev/null/" + TEST, DEFAULT);
        assertNotNull(c.getString(KEY));
        assertNotEquals(c.getString(KEY), "default");
        assertNotEquals(c.getString(KEY), "test");

        // Default = test
        c = target.create(VERSION, SITE, TEST);
        assertNotNull(c.getString(KEY));
        assertNotEquals(c.getString(KEY), "default");
        assertNotEquals(c.getString(KEY), "test");

        // Invalid path
        try {
            target.create("/tmp/" + TEST, SITE, DEFAULT);
            fail();
        } catch (RuntimeException e) {
            // Success
        }

    }

}
