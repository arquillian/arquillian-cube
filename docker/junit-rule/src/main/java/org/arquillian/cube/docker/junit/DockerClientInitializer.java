package org.arquillian.cube.docker.junit;

import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.client.CubeDockerConfigurationResolver;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.util.DefaultDocker;
import org.arquillian.cube.docker.impl.util.OperatingSystemResolver;
import org.arquillian.cube.docker.impl.util.Top;
import org.jboss.arquillian.core.api.Injector;

import java.util.HashMap;
import java.util.Map;

public class DockerClientInitializer {

    public static DockerClientExecutor initialize() {

        Injector injector = new Injector() {
            @Override
            public <T> T inject(T t) {
                return t;
            }
        };

        CubeDockerConfigurationResolver resolver = new CubeDockerConfigurationResolver(new Top(),
            new DefaultDocker(),
            new OperatingSystemResolver().currentOperatingSystem());

        final Map<String, String> config = resolver.resolve(new HashMap<>());

        final CubeDockerConfiguration cubeDockerConfiguration = CubeDockerConfiguration.fromMap(config, injector);
        return new DockerClientExecutor(cubeDockerConfiguration);
    }

}
