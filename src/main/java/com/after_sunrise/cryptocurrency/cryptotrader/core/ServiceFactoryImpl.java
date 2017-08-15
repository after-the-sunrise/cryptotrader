package com.after_sunrise.cryptocurrency.cryptotrader.core;

import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Supplier;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class ServiceFactoryImpl implements ServiceFactory {

    private final Injector injector;

    @Inject
    public ServiceFactoryImpl(Injector injector) {
        this.injector = injector;
    }

    @Override
    public <T> List<T> load(Class<T> clazz) {

        log.info("Loading service list : {}", clazz);

        ServiceLoader<T> loader = ServiceLoader.load(clazz);

        Iterator<T> itr = loader.iterator();

        List<T> services = new ArrayList<>();

        while (itr.hasNext()) {

            try {

                T service = itr.next();

                injector.injectMembers(service);

                services.add(service);

                log.info("Loaded service : {}", service);

            } catch (RuntimeException e) {

                log.warn("Skipped service.", e);

            }

        }

        return Collections.unmodifiableList(services);

    }

    @Override
    public <K, T extends Supplier<K>> Map<K, T> loadMap(Class<T> clazz) {

        log.info("Loading service map : {}", clazz);

        ServiceLoader<T> loader = ServiceLoader.load(clazz);

        Iterator<T> itr = loader.iterator();

        Map<K, T> services = new LinkedHashMap<>();

        while (itr.hasNext()) {

            try {

                T service = itr.next();

                injector.injectMembers(service);

                services.put(service.get(), service);

                log.info("Loaded service : {} - {}", service.get(), service);

            } catch (ServiceConfigurationError | RuntimeException e) {

                log.warn("Skipped service.", e);

            }

        }

        return Collections.unmodifiableMap(services);

    }

}
