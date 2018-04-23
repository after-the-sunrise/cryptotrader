package com.after_sunrise.cryptocurrency.cryptotrader.core;

import com.after_sunrise.cryptocurrency.cryptotrader.TestInterface;
import com.after_sunrise.cryptocurrency.cryptotrader.TestModule;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator;
import com.google.common.io.Resources;
import com.google.inject.ConfigurationException;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

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
    public void testEstimator() throws IOException, URISyntaxException {

        List<Estimator> estimators = target.load(Estimator.class);

        Path path = Paths.get(Resources.getResource(
                "META-INF/services/com.after_sunrise.cryptocurrency.cryptotrader.framework.Estimator"
        ).toURI());

        List<String> lines = Files.readAllLines(path);

        assertEquals(estimators.size(), lines.stream().filter(StringUtils::isNotEmpty).count());

    }

    @Test
    public void testLoad() throws Exception {

        List<TestInterface> services = target.load(TestInterface.class);
        assertEquals(services.size(), 2);

        for (TestInterface s : services) {

            if (s.getClass() == TestInterface.TestImpl1.class ||
                    s.getClass() == TestInterface.TestImpl3.class) {

                assertNotNull(s.getInjector());

                assertNotNull(s.getInjector().getInstance(ImmutableConfiguration.class));

                try {
                    s.getInjector().getInstance(ServiceFactory.class);
                    fail();
                } catch (ConfigurationException e) {
                    // Success
                }

                continue;

            }

            fail("Unknown class : " + s.get());

        }

    }

    @Test
    public void testLoadMap() throws Exception {

        Map<String, TestInterface> services = target.loadMap(TestInterface.class);
        assertTrue(services.get("TestImpl1") instanceof TestInterface.TestImpl1);
        assertTrue(services.get("TestImpl3") instanceof TestInterface.TestImpl3);
        assertEquals(services.size(), 2, services.toString());

        for (TestInterface s : services.values()) {

            assertNotNull(s.getInjector());

            assertNotNull(s.getInjector().getInstance(ImmutableConfiguration.class));

            try {
                s.getInjector().getInstance(ServiceFactory.class);
                fail();
            } catch (ConfigurationException e) {
                // Success
            }

        }

    }

}
