package com.after_sunrise.cryptocurrency.cryptotrader.core;

import com.after_sunrise.cryptocurrency.cryptotrader.TestInterface;
import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class ServiceFactoryImplTest {

    private ServiceFactoryImpl target;

    private TestModule module;

    @BeforeMethod
    public void setUp() throws Exception {

        module = new TestModule();

        target = new ServiceFactoryImpl(module.createInjector());

    }

    @Test
    public void testLoad() throws Exception {

        List<TestInterface> services = target.load(TestInterface.class);
        assertEquals(services.size(), 2);

        Set<Class<?>> classes = services.stream().map(Object::getClass).collect(Collectors.toSet());
        assertEquals(classes.size(), 2);
        assertTrue(classes.contains(TestInterface.TestImpl1.class));
        assertTrue(classes.contains(TestInterface.TestImpl3.class));

    }

    @Test
    public void testLoadMap() throws Exception {

        Map<String, TestInterface> services = target.loadMap(TestInterface.class);
        assertEquals(services.size(), 2);
        assertTrue(services.get("TestImpl1") instanceof TestInterface.TestImpl1);
        assertTrue(services.get("TestImpl3") instanceof TestInterface.TestImpl3);

    }

}
