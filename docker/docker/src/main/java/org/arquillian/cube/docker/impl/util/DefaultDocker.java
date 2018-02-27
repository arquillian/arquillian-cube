package org.arquillian.cube.docker.impl.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;

public class DefaultDocker {

    public DockerClient getDefaultDockerClient(String defaultPath) {
        final DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig
            .createDefaultConfigBuilder();
        configBuilder.withDockerHost(defaultPath);
        return DockerClientBuilder.getInstance(configBuilder.build()).build();
    }
}
