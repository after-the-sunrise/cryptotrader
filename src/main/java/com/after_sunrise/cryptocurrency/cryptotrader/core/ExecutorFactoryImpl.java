package com.after_sunrise.cryptocurrency.cryptotrader.core;

import lombok.extern.slf4j.Slf4j;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ONE;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class ExecutorFactoryImpl implements UncaughtExceptionHandler, ExecutorFactory {

    private static class ThreadFactoryImpl implements ThreadFactory {

        private static final String NAME_SUFFIX = "_%03d";

        private final ThreadFactory delegate = Executors.defaultThreadFactory();

        private final AtomicLong count = new AtomicLong();

        private final String name;

        private final UncaughtExceptionHandler handler;

        private ThreadFactoryImpl(Class<?> cls, UncaughtExceptionHandler handler) {
            this.name = cls.getSimpleName() + NAME_SUFFIX;
            this.handler = handler;
        }

        @Override
        public Thread newThread(Runnable r) {

            Thread t = delegate.newThread(r);

            t.setDaemon(true);

            t.setName(String.format(name, count.incrementAndGet()));

            t.setUncaughtExceptionHandler(handler);

            return t;

        }

    }

    private final Map<Class<?>, ScheduledExecutorService> services = new IdentityHashMap<>();

    private final Lock lock = new ReentrantLock();

    @Override
    public void uncaughtException(Thread t, Throwable e) {

        String threadName = t.getName();

        log.warn("Uncaught exception : " + threadName, e);

    }

    @Override
    public ExecutorService get(Class<?> clazz, int size) {

        Class<?> cls = clazz == null ? getClass() : clazz;

        int adjustedSize = Math.min(Math.max(INTEGER_ONE, size), Byte.MAX_VALUE);

        try {

            lock.lock();

            return services.computeIfAbsent(cls, c -> {

                log.debug("Creating executor : {} (size = {})", c.getSimpleName(), adjustedSize);

                ThreadFactory factory = new ThreadFactoryImpl(c, this);

                return Executors.newScheduledThreadPool(adjustedSize, factory);

            });

        } finally {
            lock.unlock();
        }

    }

    @Override
    public void close() throws Exception {

        try {

            lock.lock();

            int count = 0;

            for (Class<?> clz : services.keySet().toArray(new Class[0])) {

                ExecutorService service = services.remove(clz);

                log.debug("Terminating executor : {}", clz.getSimpleName());

                service.shutdown();

                count++;

            }

            log.debug("Terminated {} executors.", count);

        } finally {
            lock.unlock();
        }

    }

}
