package org.arquillian.cube.docker.impl.client;

import java.util.Map;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeDockerRegistrar {

    @SuppressWarnings("unchecked")
    public void register(@Observes DockerClientExecutor executor, CubeDockerConfiguration configuration, Injector injector, CubeRegistry registry) {

        //TODO, add key here generation here
        Map<String, Object> containerConfigurations = configuration.getDockerContainersContent();
        for(Map.Entry<String, Object> containerConfiguration : containerConfigurations.entrySet()) {

            registry.addCube(
                    injector.inject(
                        new DockerCube(
                                containerConfiguration.getKey(),
                                (Map<String, Object>)containerConfiguration.getValue(),
                                executor)));
        }
    }
}
