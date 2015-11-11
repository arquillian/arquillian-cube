package org.arquillian.cube.docker.impl.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ConfigUtil {

    private ConfigUtil() {}

    public static String[] trim(String[] data) {
        List<String> result = new ArrayList<String>();
        for(String val : data) {
            String trimmed = val.trim();
            if(!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result.toArray(new String[]{});
    }

    public static String[] reverse(String[] cubeIds) {
        String[] result = new String[cubeIds.length];
        int n = cubeIds.length-1;
        for(int i = 0; i < cubeIds.length; i++) {
            result[n--] = cubeIds[i];
        }
        return result;
    }

    public static Map<String, Object> applyExtendsRules(Map<String, Object> containerConfig) {
        for(Map.Entry<String, Object> containerEntry : containerConfig.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> container = (Map<String, Object>)containerEntry.getValue();
            if(container.containsKey("extends")) {
                String extendsContainer = container.get("extends").toString();
                if(!containerConfig.containsKey(extendsContainer)) {
                    throw new IllegalArgumentException(containerEntry.getKey() + " extends a non existing container definition " + extendsContainer);
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> extendedContainer = (Map<String, Object>)containerConfig.get(extendsContainer);
                for(Map.Entry<String, Object> extendedContainerEntry : extendedContainer.entrySet()) {
                    if(!container.containsKey(extendedContainerEntry.getKey())) {
                        container.put(extendedContainerEntry.getKey(), extendedContainerEntry.getValue());
                    }
                }
            }
        }
        return containerConfig;
    }
}
