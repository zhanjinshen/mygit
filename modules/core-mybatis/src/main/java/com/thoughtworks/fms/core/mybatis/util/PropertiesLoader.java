package com.thoughtworks.fms.core.mybatis.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import static java.nio.charset.Charset.defaultCharset;

public class PropertiesLoader {

    private static final String DEFAULT_PROPERTY_NAME = "fms.properties";
    private static Properties DEFAULT_PROPERTIES;

    static {
        DEFAULT_PROPERTIES = loadProperties(DEFAULT_PROPERTY_NAME);
    }

    public static Properties loadProperties(String propertyFilename) {
        return new PropertiesLoader().getProperties(propertyFilename);
    }

    public static String getProperty(String key) {
        return DEFAULT_PROPERTIES.getProperty(key);
    }

    private Properties getProperties(String fileName) {
        final Properties properties = new Properties();
        try (InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            properties.load(new InputStreamReader(resourceAsStream, defaultCharset()));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return properties;
    }

}
