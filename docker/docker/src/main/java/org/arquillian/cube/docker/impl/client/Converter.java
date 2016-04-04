package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.client.config.CubeContainers;

public interface Converter {
    CubeContainers convert();
}
