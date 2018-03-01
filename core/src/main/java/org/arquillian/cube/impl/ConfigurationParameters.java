package org.arquillian.cube.impl;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationParameters {

    private Map<String, String> configParams;

    public ConfigurationParameters(Map<String, String> configParams) {
        this.configParams = configParams;
    }

    Map<String, String> getConfigParams() {
        return configParams;
    }

    public static ConfigurationParameters from(String key, String value) {
        final HashMap<String, String> map = new HashMap<>();
        map.put(key, value);

        return new ConfigurationParameters(map);
    }
}
