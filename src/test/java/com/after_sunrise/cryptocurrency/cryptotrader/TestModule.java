package com.after_sunrise.cryptocurrency.cryptotrader;

import com.after_sunrise.cryptocurrency.cryptotrader.core.ExecutorFactory;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class TestModule {

    private final Map<Class<?>, Object> mocks = new HashMap<>();

    private final Injector injector;

    private Instant now = null;

    public TestModule() {

        this.injector = Guice.createInjector();

        ExecutorService service = setMock(ExecutorService.class, newDirectExecutorService());

        when(getMock(ExecutorFactory.class).get(any(Class.class), anyInt())).thenReturn(service);

    }

    public void setNow(Instant now) {
        this.now = now;
    }

    public Instant getNow() {
        return now == null ? Instant.now() : now;
    }

    public <T> T getMock(Class<T> clz) {
        return clz.cast(mocks.computeIfAbsent(clz, Mockito::mock));
    }

    public <T> T setMock(Class<T> clz, T mock) {

        mocks.put(clz, mock);

        return mock;

    }

    public Injector createInjector() {

        Injector mock = mock(Injector.class);

        doAnswer(invocation -> {

            Class<?> c = invocation.getArgumentAt(0, Class.class);

            return getMock(c);

        }).when(mock).getInstance(Mockito.<Class<?>>any());

        doAnswer(invocation -> {

            injector.injectMembers(invocation.getArguments()[0]);

            return null;

        }).when(mock).injectMembers(any());

        return mock;

    }

}
