package org.arquillian.cube.impl.util;

public class SystemEnvironmentVariables {

    private static final String DOT_OR_DASH = "[.-]";
    private static final String UNDERSCORE = "_";

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

        //The reason that we also check against empty, is that test use surefire to build a predictable environment
        //and surefire doesn't let you set an environment variable to `null`. So we treat them the same.
        if (Strings.isNullOrEmpty(result)) {
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

    public static String getPropertyOrEnvironmentVariable(String property, String variable, String defaultValue) {
        String answer = System.getProperty(property);
        if (Strings.isNotNullOrEmpty(answer)) {
            return answer;
        } else {
            answer = System.getenv(variable);
            return Strings.isNotNullOrEmpty(answer) ? answer : defaultValue;
        }
    }

    public static String getPropertyOrEnvironmentVariable(String property, String defaultValue) {
        return getPropertyOrEnvironmentVariable(property, propertyToEnvironmentVariableName(property), defaultValue);
    }

    public static String getPropertyOrEnvironmentVariable(String property) {
        return getPropertyOrEnvironmentVariable(property, (String) null);
    }

    public static boolean getPropertyOrEnvironmentVariable(String property, boolean defaultValue) {
        String result = getPropertyOrEnvironmentVariable(property, new Boolean(defaultValue).toString());
        return Boolean.parseBoolean(result);
    }

    public static int getPropertyOrEnvironmentVariable(String property, int defaultValue) {
        String result = getPropertyOrEnvironmentVariable(property, new Integer(defaultValue).toString());
        return Integer.parseInt(result);
    }

    public static long getPropertyOrEnvironmentVariable(String property, long defaultValue) {
        String result = getPropertyOrEnvironmentVariable(property, new Long(defaultValue).toString());
        return Long.parseLong(result);
    }

    public static String propertyToEnvironmentVariableName(String property) {
        return property.toUpperCase().replaceAll(DOT_OR_DASH, UNDERSCORE);
    }
}
