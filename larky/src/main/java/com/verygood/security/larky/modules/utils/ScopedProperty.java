package com.verygood.security.larky.modules.utils;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Nonnull;


/**
 * Utility to make it easier for tests to override system properties (instead of directly using
 * {@link System#setProperty(String, String)}). By using this class within try-with-resources,
 * it can be assured that any property set will be restored to its initial state after the
 * exiting the scope.
 */
public class ScopedProperty implements AutoCloseable {
    private final Map<String, String> properties = new HashMap<>();

    /**
     * Sets a system property (by calling {@link System#setProperty(String, String)}) while
     * backing up the original value and replacing it once this class is closed.
     */
    public void setProperty(@Nonnull String key, String value) {
        if (!properties.containsKey(key)) {
            String originalValue = System.getProperty(key);
            properties.put(key, originalValue);
        }
        if (value == null) {
            // setProperty doesn't support null values so use clearProperty instead in that case.
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    @Override
    public void close() throws Exception {
        for (Map.Entry<String, String> original : properties.entrySet()) {
            // setProperty doesn't support null values so use clearProperty instead in that case.
            if (original.getValue() == null) {
                System.clearProperty(original.getKey());
            } else {
                System.setProperty(original.getKey(), original.getValue());
            }
        }
    }
}