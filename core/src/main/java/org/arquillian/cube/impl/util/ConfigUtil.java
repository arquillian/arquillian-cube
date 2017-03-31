package org.arquillian.cube.impl.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.arquillian.cube.impl.util.SystemEnvironmentVariables.getPropertyOrEnvironmentVariable;

public class ConfigUtil {

    /**
     * Gets a property from system, environment or an external map.
     * The lookup order is system > env > map > defaultValue.
     *
     * @param name
     *     The name of the property.
     * @param map
     *     The external map.
     * @param defaultValue
     *     The value that should be used if property is not found.
     */
    public static String getStringProperty(String name, Map<String, String> map, String defaultValue) {
        if (map.containsKey(name)) {
            return map.get(name);
        } else {
            return getPropertyOrEnvironmentVariable(name, defaultValue);
        }
    }

    /**
     * Gets a property from system, environment or an external map. This method supports also passing an alternative
     * name.
     * The reason for supporting multiple names, is to support multiple keys for the same property (e.g. adding a new and
     * deprecating the old).
     * The lookup order is system[name] > env[name] > map[name] > system[alternativeName] > env[alternativeName] >
     * map[alternativeName] > defaultValue.
     *
     * @param name
     *     The name of the property.
     * @param alternativeName
     *     An alternate name to use.
     * @param map
     *     The external map.
     * @param defaultValue
     *     The value that should be used if property is not found.
     */
    public static String getStringProperty(String name, String alternativeName, Map<String, String> map,
        String defaultValue) {
        return getStringProperty(name, map, getStringProperty(alternativeName, map, defaultValue));
    }

    public static Boolean getBooleanProperty(String name, Map<String, String> map, Boolean defaultValue) {
        if (map.containsKey(name)) {
            return Boolean.parseBoolean(map.get(name));
        } else {
            return getPropertyOrEnvironmentVariable(name, defaultValue);
        }
    }

    public static Long getLongProperty(String name, Map<String, String> map, Long defaultValue) {
        if (map.containsKey(name)) {
            return Long.parseLong(map.get(name));
        } else {
            return Long.parseLong(getPropertyOrEnvironmentVariable(name, String.valueOf(defaultValue)));
        }
    }

    public static URL[] asURL(List<String> stringUrls) {
        List<URL> urls = new ArrayList<>();
        for (String stringUrl : stringUrls) {
            try {
                urls.add(new URL(stringUrl));
            } catch (MalformedURLException e) {
                //Just ignore
            }
        }
        return urls.toArray(new URL[urls.size()]);
    }
}
