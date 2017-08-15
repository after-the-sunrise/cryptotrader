package com.after_sunrise.cryptocurrency.cryptotrader.core;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static java.lang.Thread.currentThread;
import static org.testng.Assert.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class ExecutorFactoryImplTest {

    private ExecutorFactoryImpl target;

    @BeforeMethod
    public void setUp() {
        target = new ExecutorFactoryImpl();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        target.close();
    }

    @Test(timeOut = 5000L)
    public void testGet() throws Exception {

        List<Class<?>> classes = Arrays.asList(String.class, null, Integer.class);

        List<ExecutorService> services = new ArrayList<>();

        for (Class<?> cls : classes) {

            ExecutorService es = target.get(cls, 1);
            assertSame(target.get(cls, 0), es);
            assertSame(target.get(cls, 1), es);
            assertSame(target.get(cls, 2), es);

            services.add(es);

            Class<?> c = (cls == null) ? target.getClass() : cls;

            assertEquals(es.submit(() -> currentThread().getName()).get(), c.getSimpleName() + "_001");

            assertSame(es.submit(() -> currentThread().getUncaughtExceptionHandler()).get(), target);

            assertTrue(es.submit(() -> currentThread().isDaemon()).get());

            es.submit(() -> {
                throw new RuntimeException("Test Exception");
            });

            es.submit(() -> "Wait Termination").get();


        }

        target.close();

        for (ExecutorService es : services) {

            assertTrue(es.isShutdown());

        }

    }

    @Test
    public void testUncaughtException() {
        target.uncaughtException(currentThread(), new IOException("test"));
    }

}
