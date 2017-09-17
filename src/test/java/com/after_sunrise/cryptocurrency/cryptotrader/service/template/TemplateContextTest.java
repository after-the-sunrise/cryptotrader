package com.after_sunrise.cryptocurrency.cryptotrader.service.template;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.test.TestPortProvider;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class TemplateContextTest {

    private static class TestContext extends TemplateContext {
        private TestContext() {
            super("test");
        }
    }

    @Path("/")
    @Produces("application/json")
    public static class TestApplication extends Application {

        @Override
        public Set<Object> getSingletons() {
            return Collections.singleton(this);
        }

        @GET
        @Path("/foo")
        public String getFoo() {
            return "{foo:bar}";
        }

        @GET
        @Path("/bar")
        public String getBar() throws IOException {
            throw new IOException("test");
        }

    }

    private TemplateContext target;

    @BeforeMethod
    public void setUp() {
        target = spy(new TestContext());
    }

    @Test
    public void testGet() throws Exception {
        assertEquals(target.get(), "test");
    }

    @Test
    public void testClose() throws Exception {
        target.close();
    }

    @Test
    public void testQuery() throws IOException {

        UndertowJaxrsServer server = new UndertowJaxrsServer().start();

        try {

            String url = "http://localhost:" + TestPortProvider.getPort();

            server.deploy(TestApplication.class);

            assertEquals(target.query(url + "/foo"), "{foo:bar}");

            assertEquals(target.query(url + "/bar"), null);

        } finally {
            server.stop();
        }

    }

    @Test
    public void testFindCached() throws Exception {

        Key key = Key.from(null);
        Callable<BigDecimal> callable = mock(Callable.class);
        when(callable.call()).thenReturn(ONE, TEN, null);

        assertEquals(target.findCached(BigDecimal.class, key, callable), ONE);
        assertEquals(target.findCached(BigDecimal.class, key, callable), ONE);
        verify(callable).call();

        target.clear();
        assertEquals(target.findCached(BigDecimal.class, key, callable), TEN);
        assertEquals(target.findCached(BigDecimal.class, key, callable), TEN);
        verify(callable, times(2)).call();

        target.clear();
        assertNull(target.findCached(BigDecimal.class, key, callable));
        assertNull(target.findCached(BigDecimal.class, key, callable));
        verify(callable, times(3)).call();

        target.clear();
        doThrow(new Exception("test")).when(callable).call();
        assertNull(target.findCached(BigDecimal.class, key, callable));
        assertNull(target.findCached(BigDecimal.class, key, callable));
        verify(callable, times(5)).call();

        target.clear();
        assertNull(target.findCached(null, key, callable));
        assertNull(target.findCached(BigDecimal.class, null, callable));
        verifyNoMoreInteractions(callable);

    }

    @Test
    public void testListCached() throws Exception {

        Key key = Key.from(null);
        Callable<List<BigDecimal>> callable = mock(Callable.class);
        when(callable.call()).thenReturn(singletonList(ONE), singletonList(TEN), null);

        assertEquals(target.listCached(BigDecimal.class, key, callable), singletonList(ONE));
        assertEquals(target.listCached(BigDecimal.class, key, callable), singletonList(ONE));
        verify(callable).call();

        target.clear();
        assertEquals(target.listCached(BigDecimal.class, key, callable), singletonList(TEN));
        assertEquals(target.listCached(BigDecimal.class, key, callable), singletonList(TEN));
        verify(callable, times(2)).call();

        target.clear();
        assertEquals(target.listCached(BigDecimal.class, key, callable), emptyList());
        assertEquals(target.listCached(BigDecimal.class, key, callable), emptyList());
        verify(callable, times(3)).call();

        target.clear();
        doThrow(new Exception("test")).when(callable).call();
        assertEquals(target.listCached(BigDecimal.class, key, callable), emptyList());
        assertEquals(target.listCached(BigDecimal.class, key, callable), emptyList());
        verify(callable, times(5)).call();

        target.clear();
        assertEquals(target.listCached(null, key, callable), emptyList());
        assertEquals(target.listCached(BigDecimal.class, null, callable), emptyList());
        verifyNoMoreInteractions(callable);

    }

    @Test
    public void testGetQuietly() throws Exception {

        CompletableFuture<BigDecimal> future = null;
        Duration timeout = null;
        assertNull(target.getQuietly(future, timeout));

        future = CompletableFuture.completedFuture(ONE);
        assertEquals(target.getQuietly(future, timeout), ONE);

        timeout = Duration.ofMillis(1);
        assertEquals(target.getQuietly(future, timeout), ONE);

        future = null;
        timeout = Duration.ofMillis(1);
        assertNull(target.getQuietly(future, timeout));

        future = new CompletableFuture<>();
        future.completeExceptionally(new Exception("test"));
        assertNull(target.getQuietly(future, timeout));

        timeout = null;
        assertNull(target.getQuietly(future, timeout));

    }

    @Test
    public void testGetMidPrice() throws Exception {

        Key key = Key.builder().instrument("foo").build();

        doReturn(null).when(target).getBestAskPrice(key);
        doReturn(null).when(target).getBestBidPrice(key);
        assertNull(target.getMidPrice(key));

        doReturn(BigDecimal.TEN).when(target).getBestAskPrice(key);
        doReturn(null).when(target).getBestBidPrice(key);
        assertNull(target.getMidPrice(key));

        doReturn(null).when(target).getBestAskPrice(key);
        doReturn(BigDecimal.ONE).when(target).getBestBidPrice(key);
        assertNull(target.getMidPrice(key));

        doReturn(BigDecimal.TEN).when(target).getBestAskPrice(key);
        doReturn(BigDecimal.ONE).when(target).getBestBidPrice(key);
        assertEquals(target.getMidPrice(key), new BigDecimal("5.5"));

    }

    @Test
    public void testInterfaceMethods() throws ReflectiveOperationException {

        for (Method m : Context.class.getMethods()) {

            if (m.getDeclaringClass() != Context.class) {
                continue;
            }

            Object[] args = new Object[m.getParameterTypes().length];

            Object result = m.invoke(target, args);

            assertNull(result, m.getName());

        }

    }

}
