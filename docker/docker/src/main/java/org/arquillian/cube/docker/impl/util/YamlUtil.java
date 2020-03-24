package org.arquillian.cube.docker.impl.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    /**
     * This method converts a map of key:value pairs into a list of 'key=value' strings
     *
     * @param mapOfStrings Map of key:value pairs
     * @return List of key=value strings
     */
    public static final List<String> asListOfString(Map<String, ?> mapOfStrings) {
        ArrayList<String> result = new ArrayList<>();
        for (Map.Entry<String, ?> ent: mapOfStrings.entrySet()) {
            result.add(ent.getKey() + "=" + (ent.getValue() != null ? ent.getValue() : ""));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
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

    public static final long asLong(Map<String, Object> map, String property) {
        return (long) map.get(property);
    }
}
