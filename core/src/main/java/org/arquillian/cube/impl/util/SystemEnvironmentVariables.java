package org.arquillian.cube.impl.util;

public class SystemEnvironmentVariables {

    public static final String getPropertyVariable(String property) {
        return getPropertyVariable(property, null);
    }

    public static final String getPropertyVariable(String property, String defaultValue) {
        return System.getProperty(property, defaultValue);
    }

    public static final String getEnvironmentVariable(String property) {
        return getEnvironmentVariable(property, null);
    }

    public static final String getEnvironmentVariable(String property, String defaultValue) {
        String result = System.getenv(property);
        if (result == null) {
            return defaultValue;
        } else {
            return result;
        }
    }

    public static final String getEnvironmentOrPropertyVariable(String property) {
        return getEnvironmentOrPropertyVariable(property, null);
    }


    public static final String getEnvironmentOrPropertyVariable(String property, String defaultValue) {
        return getEnvironmentVariable(property, getPropertyVariable(property, defaultValue));
    }
}
