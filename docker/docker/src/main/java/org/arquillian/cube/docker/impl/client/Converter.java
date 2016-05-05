package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.client.config.DockerCompositions;

public interface Converter {
    DockerCompositions convert();
}
