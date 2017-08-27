package com.after_sunrise.cryptocurrency.cryptotrader.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Supplier;

import static java.lang.System.getProperty;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class ConfigurationSupplier implements Supplier<Configuration> {

    static final String VERSION = "cryptotrader-version.properties";

    static final String SITE = getProperty("user.home") + getProperty("file.separator") + ".cryptotrader";

    static final String DEFAULT = "cryptotrader-default.properties";

    @Override
    public Configuration get() {
        return get(VERSION, SITE, DEFAULT);
    }

    @VisibleForTesting
    Configuration get(String versionPath, String sitePath, String defaultPath) {

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
