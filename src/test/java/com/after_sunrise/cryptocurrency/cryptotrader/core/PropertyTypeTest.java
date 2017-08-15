package com.after_sunrise.cryptocurrency.cryptotrader.core;

import com.google.common.io.Resources;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.assertTrue;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class PropertyTypeTest {

    @Test
    public void testGetKey() throws Exception {

        Set<String> keys = new HashSet<>();

        for (PropertyType type : PropertyType.values()) {

            String key = type.getKey();

            assertTrue(keys.add(key), "Duplicate : " + key);

        }

    }

    @Test
    public void testDefaults() throws Exception {

        URL url = Resources.getResource("cryptotrader-default.properties");

        Configuration c = new Configurations().properties(url);

        for (PropertyType type : PropertyType.values()) {

            String key = type.getKey();

            assertTrue(c.containsKey(key), "Missing default : " + key);

        }

    }

}
