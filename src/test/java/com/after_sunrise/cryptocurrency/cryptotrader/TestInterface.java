package com.after_sunrise.cryptocurrency.cryptotrader;

import com.google.inject.Inject;
import com.google.inject.Injector;

import java.util.function.Supplier;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public interface TestInterface extends Supplier<String> {

    Injector getInjector();

    class TestImpl1 implements TestInterface {

        private Injector injector;

        @Inject
        public void initialize(Injector injector) {
            this.injector = injector;
        }

        @Override
        public Injector getInjector() {
            return injector;
        }

        @Override
        public String get() {
            return getClass().getSimpleName();
        }
    }

    class TestImpl2 extends TestImpl1 implements TestInterface {
        @Inject
        public void initialize(Injector injector) {
            throw new RuntimeException("test");
        }
    }

    class TestImpl3 extends TestImpl1 implements TestInterface {
    }

}
