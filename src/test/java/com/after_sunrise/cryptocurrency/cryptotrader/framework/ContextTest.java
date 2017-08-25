package com.after_sunrise.cryptocurrency.cryptotrader.framework;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
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

        Request.RequestBuilder builder = Request.builder();
        builder = builder.site("s");
        builder = builder.instrument("i");
        builder = builder.currentTime(Instant.ofEpochMilli(0L));
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
