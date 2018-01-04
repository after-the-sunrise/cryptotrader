package com.after_sunrise.cryptocurrency.cryptotrader.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.System.getProperty;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class ConfigurationProviderImpl implements ConfigurationProvider, InvocationHandler {

    static final String VERSION = "cryptotrader-version.properties";

    static final String SITE = getProperty("user.home") + getProperty("file.separator") + ".cryptotrader";

    static final String DEFAULT = "cryptotrader-default.properties";

    private final AtomicReference<Configuration> reference = new AtomicReference<>();

    private final Configuration proxy;

    public ConfigurationProviderImpl() {

        ClassLoader cl = getClass().getClassLoader();

        Object p = Proxy.newProxyInstance(cl, new Class[]{Configuration.class}, this);

        proxy = Configuration.class.cast(p);

    }

    @Override
    public Configuration get() {
        return proxy;
    }

    @Override
    public void clear() {

        log.debug("Clearing cached.");

        reference.set(null);

    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Configuration cached = reference.get();

        if (cached == null) {

            log.debug("Creating cache.");

            cached = create(VERSION, SITE, DEFAULT);

            reference.set(cached);

        }

        return method.invoke(cached, args);

    }

    @VisibleForTesting
    Configuration create(String versionPath, String sitePath, String defaultPath) {

        CompositeConfiguration composite = new CompositeConfiguration();

        try {

            createClasspath(versionPath).ifPresent(composite::addConfiguration);

            createSystem().ifPresent(composite::addConfiguration);

            createFilePath(sitePath).ifPresent(composite::addConfiguration);

            createClasspath(defaultPath).ifPresent(composite::addConfiguration);

        } catch (Exception e) {

            throw new RuntimeException("Failed to load configurations.", e);

        }

        return composite;

    }

    private Optional<Configuration> createClasspath(String path) throws ConfigurationException {

        log.debug("Loading classpath configuration : {}", path);

        URL url = Resources.getResource(path);

        return Optional.of(new Configurations().properties(url));

    }

    private Optional<Configuration> createSystem() {
        return Optional.of(new SystemConfiguration());
    }

    private Optional<Configuration> createFilePath(String path) throws Exception {

        Path file = Paths.get(path);

        if (!Files.exists(file)) {

            log.warn("Skipped filepath configuration : {}", path);

            return Optional.empty();

        }

        log.debug("Loading filepath configuration : {}", path);

        URL url = file.toUri().toURL();

        return Optional.of(new Configurations().properties(url));

    }

}
