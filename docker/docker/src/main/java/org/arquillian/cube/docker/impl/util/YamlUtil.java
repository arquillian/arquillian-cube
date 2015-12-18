package org.arquillian.cube.docker.impl.util;

import java.util.Collection;
import java.util.Map;

public class YamlUtil {

    private YamlUtil() {
        super();
    }

    public static final boolean asBoolean(Map<String, Object> map, String property) {
        return (boolean) map.get(property);
    }

    @SuppressWarnings("unchecked")
    public static final Collection<Map<String, Object>> asListOfMap(Map<String, Object> map, String property) {
        return (Collection<Map<String, Object>>) map.get(property);
    }

    @SuppressWarnings("unchecked")
    public static final Collection<String> asListOfString(Map<String, Object> map, String property) {
        return (Collection<String>) map.get(property);
    }

    public static final String asString(Map<String, Object> map, String property) {
        return (String) map.get(property);
    }

    @SuppressWarnings("unchecked")
    public static final Map<String, Object> asMap(Map<String, Object> map, String property) {
        return (Map<String, Object>) map.get(property);
    }

    @SuppressWarnings("unchecked")
    public static final Map<String, String> asMapOfStrings(Map<String, Object> map, String property) {
        return (Map<String, String>) map.get(property);
    }

    public static final int asInt(Map<String, Object> map, String property) {
        return (int) map.get(property);
    }
    
}
