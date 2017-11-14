package com.after_sunrise.cryptocurrency.cryptotrader;

import com.after_sunrise.cryptocurrency.cryptotrader.core.ExecutorFactory;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Request;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static com.google.common.io.Resources.getResource;
import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class TestModule {

    private static final String CONFIGURATION = "cryptotrader-test.properties";

    private final Map<Class<?>, Object> mocks = new HashMap<>();

    private final Injector injector;

    public TestModule() throws ConfigurationException {

        this.injector = Guice.createInjector();

        ExecutorService service = setMock(ExecutorService.class, newDirectExecutorService());

        when(getMock(ExecutorFactory.class).get(any(Class.class), anyInt())).thenReturn(service);

        Configuration configuration = spy(new Configurations().properties(getResource(CONFIGURATION)));

        setMock(Configuration.class, configuration);

        setMock(ImmutableConfiguration.class, configuration);

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

    public Request.RequestBuilder createRequestBuilder() {

        Instant now = Instant.now();

        Request.RequestBuilder builder = Request.builder()
                .site("s")
                .instrument("i")
                .currentTime(now)
                .targetTime(now.plus(Duration.ofMillis(5)))
                .tradingExposure(new BigDecimal("0.10"))
                .tradingSplit(2)
                .tradingSpread(new BigDecimal("0.0060"))
                .tradingDuration(Duration.ofMinutes(1));

        return builder;

    }

}
