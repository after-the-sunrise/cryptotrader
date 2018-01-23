package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.testng.Assert.assertEquals;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class ConstantContextTest {

    private ConstantContext target;

    @BeforeMethod
    public void setUp() throws Exception {
        target = new ConstantContext();
    }

    @Test
    public void testConvertDecimal() {

        Key.KeyBuilder b = Key.builder().site(ConstantContext.ID).timestamp(Instant.now());

        Key key = b.instrument("1.0").build();
        assertEquals(target.convertDecimal(key), new BigDecimal("1.0"));
        assertEquals(target.convertDecimal(key), new BigDecimal("1.0"));

        key = b.instrument("-1.2").build();
        assertEquals(target.convertDecimal(key), new BigDecimal("-1.2"));
        assertEquals(target.convertDecimal(key), new BigDecimal("-1.2"));

        key = b.instrument("foo").build();
        assertEquals(target.convertDecimal(key), null);
        assertEquals(target.convertDecimal(key), null);

        key = b.instrument(null).build();
        assertEquals(target.convertDecimal(key), null);
        assertEquals(target.convertDecimal(key), null);

    }

}
