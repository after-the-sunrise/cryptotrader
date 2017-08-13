package com.after_sunrise.cryptocurrency.cryptotrader.framework.impl;

import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Context.Key;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Objects;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
@Slf4j
public class ContextProvider implements Provider<Context>, InvocationHandler {

    private final Context delegate;

    private final Map<String, Context> contexts;

    @Inject
    public ContextProvider(Injector injector) {

        ClassLoader loader = getClass().getClassLoader();

        Class<?>[] interfaces = new Class[]{Context.class};

        this.delegate = (Context) Proxy.newProxyInstance(loader, interfaces, this);

        this.contexts = Frameworks.loadMap(Context.class, injector);

    }

    @Override
    public Context get() {
        return delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        for (Object arg : args) {

            if (arg instanceof Key) {

                Key key = (Key) arg;

                Context context = contexts.get(key.getSite());

                if (context == null) {

                    log.trace("Context not found for site : {}", key.getSite());

                    return null;

                }

                return method.invoke(context, args);

            }

        }

        log.trace("Invalid args : {} - {}", method, Objects.toString(args));

        return null;

    }

}
