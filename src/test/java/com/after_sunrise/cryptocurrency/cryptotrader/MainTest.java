package com.after_sunrise.cryptocurrency.cryptotrader;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.*;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class MainTest {

    @Test
    public void testMain() throws Exception {

        Main.main();

    }

    @Test
    public void testEntity() throws ReflectiveOperationException {
        test(Adviser.Advice.class);
        test(Context.Key.class);
        test(Estimator.Estimation.class);
        test(Instruction.CreateInstruction.class);
        test(Instruction.CancelInstruction.class);
        test(Trader.Request.class);
    }

    private void test(Class<?> clazz) throws ReflectiveOperationException {

        testBuilder(clazz);

        testConstructor(clazz);

    }

    private void testBuilder(Class<?> clazz) throws ReflectiveOperationException {

        // Methods for the builder itself.
        Object builder = clazz.getMethod("builder").invoke(clazz);
        assertNotNull(builder.toString());
        assertEquals(builder.hashCode(), builder.hashCode());
        assertTrue(builder.equals(builder));

        // Build instance with no parameters set.
        Object emptyEntity = testGet(builder.getClass().getMethod("build").invoke(builder));
        assertNotNull(emptyEntity.toString());
        assertEquals(emptyEntity.hashCode(), emptyEntity.hashCode());
        assertTrue(emptyEntity.equals(emptyEntity));
        assertFalse(emptyEntity.equals(clazz));
        assertFalse(emptyEntity.equals(null));

        // Possible method arguments
        Map<Class<?>, Object> args = new HashMap<>();
        args.put(String.class, String.valueOf(Math.random()));
        args.put(BigDecimal.class, BigDecimal.TEN);
        args.put(Instant.class, Instant.now());
        args.put(ZonedDateTime.class, ZonedDateTime.now());

        // Invoke parameter setting methods.
        for (Field field : clazz.getDeclaredFields()) {

            for (Method method : builder.getClass().getMethods()) {

                if (method.getReturnType() != builder.getClass()) {
                    continue;
                }

                method.invoke(builder, args.get(method.getParameterTypes()[0]));

            }

        }

        // Build instance with some parameters set.
        Object filledEntity = testGet(builder.getClass().getMethod("build").invoke(builder));
        assertNotNull(filledEntity.toString());
        assertEquals(filledEntity.hashCode(), filledEntity.hashCode());
        assertTrue(filledEntity.equals(filledEntity));
        assertFalse(filledEntity.equals(clazz));
        assertFalse(filledEntity.equals(null));

    }

    private void testConstructor(Class<?> clazz) throws ReflectiveOperationException {

        for (Constructor<?> c : clazz.getConstructors()) {

            Object[] constructorArgs = new Object[c.getParameterTypes().length];

            Object entity = testGet(c.newInstance(constructorArgs));
            assertNotNull(entity.toString());
            assertEquals(entity.hashCode(), entity.hashCode());
            assertTrue(entity.equals(entity));
            assertFalse(entity.equals(clazz));
            assertFalse(entity.equals(null));

        }

    }

    private Object testGet(Object o) throws ReflectiveOperationException {

        for (Method m : o.getClass().getMethods()) {

            if (m.getParameterCount() != 0) {
                continue;
            }

            if (!StringUtils.startsWith(m.getName(), "get")) {
                continue;
            }

            m.invoke(o);

        }

        return o;

    }

}
