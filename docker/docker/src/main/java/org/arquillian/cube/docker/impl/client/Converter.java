package org.arquillian.cube.docker.impl.client;

import java.util.Map;

public interface Converter {
    Map<String, Object> convert();
}
