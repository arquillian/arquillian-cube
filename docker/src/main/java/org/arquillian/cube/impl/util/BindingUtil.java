package org.arquillian.cube.impl.util;

import java.util.Map;

import org.arquillian.cube.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Binding;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.HostConfig;
import com.github.dockerjava.api.model.ExposedPort;

public final class BindingUtil {

    private BindingUtil() {}

    public static Binding binding(DockerClientExecutor executor, String cubeId) {
        InspectContainerResponse inspectResponse = executor.getDockerClient().inspectContainerCmd(cubeId).exec();;

        HostConfig hostConfig = inspectResponse.getHostConfig();
        String gatewayIp = inspectResponse.getNetworkSettings().getGateway();

        Binding binding = new Binding(gatewayIp);
        for (Map.Entry<ExposedPort, com.github.dockerjava.api.model.Ports.Binding> bind : hostConfig.getPortBindings().getBindings().entrySet()) {
            binding.addPortBinding(bind.getKey().getPort(), bind.getValue().getHostPort());
        }
        return binding;
    }
}
