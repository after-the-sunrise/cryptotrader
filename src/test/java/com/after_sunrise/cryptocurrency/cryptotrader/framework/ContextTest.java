package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trader.Request;
import org.testng.annotations.Test;

import java.time.Instant;

import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class ContextTest {

    @Test
    public void testKey() throws Exception {

        assertFalse(Key.isValid(null));
        assertFalse(Key.isValid(Key.from(null)));

        Request.RequestBuilder builder = Request.builder();
        assertFalse(Key.isValid(Key.from(builder.build())));

        builder = builder.site("s");
        assertFalse(Key.isValid(Key.from(builder.build())));

        builder = builder.instrument("i");
        assertFalse(Key.isValid(Key.from(builder.build())));

        builder = builder.targetTime(Instant.ofEpochMilli(0L));
        assertTrue(Key.isValid(Key.from(builder.build())));

        Key key = Key.from(builder.build());

        assertTrue(key.equals(key));
        assertTrue(key.equals(Key.from(builder.build())));
        assertFalse(key.equals(new Object()));
        assertFalse(key.equals(null));

        assertEquals(key.hashCode(), key.hashCode());
        assertEquals(key.hashCode(), Key.from(builder.build()).hashCode());

        assertEquals(key.toString(), "Context.Key(site=s, instrument=i, timestamp=1970-01-01T00:00:00Z)");

    }

}
