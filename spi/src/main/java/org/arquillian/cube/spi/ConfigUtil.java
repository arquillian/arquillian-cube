package org.arquillian.cube.spi;

import java.util.ArrayList;
import java.util.List;

final class ConfigUtil {

    private ConfigUtil() {}

    static String[] trim(String[] data) {
        List<String> result = new ArrayList<String>();
        for(String val : data) {
            String trimmed = val.trim();
            if(!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result.toArray(new String[]{});
    }

    static String[] reverse(String[] cubeIds) {
        String[] result = new String[cubeIds.length];
        int n = cubeIds.length-1;
        for(int i = 0; i < cubeIds.length; i++) {
            result[n--] = cubeIds[i];
        }
        return result;
    }
}
