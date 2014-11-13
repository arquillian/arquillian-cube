package org.arquillian.cube.impl.util;

import java.util.List;
import java.util.Map;

import org.arquillian.cube.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Binding;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.HostConfig;
import com.github.dockerjava.api.model.ExposedPort;

public final class BindingUtil {

    private static final String NO_GATEWAY = null;

    private BindingUtil() {
    }

    public static Binding binding(DockerClientExecutor executor, String cubeId) {
        InspectContainerResponse inspectResponse = executor.getDockerClient().inspectContainerCmd(cubeId).exec();
        ;

        HostConfig hostConfig = inspectResponse.getHostConfig();
        String gatewayIp = inspectResponse.getNetworkSettings().getGateway();

        Binding binding = new Binding(gatewayIp);
        for (Map.Entry<ExposedPort, com.github.dockerjava.api.model.Ports.Binding> bind : hostConfig.getPortBindings()
                .getBindings().entrySet()) {
            binding.addPortBinding(bind.getKey().getPort(), bind.getValue().getHostPort());
        }
        return binding;
    }

    public static Binding binding(Map<String, Object> cubeConfiguration) {

        Binding binding = new Binding(NO_GATEWAY);

        if (cubeConfiguration.containsKey("portBindings")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cubePortBindings = (List<Map<String, Object>>) cubeConfiguration
                    .get("portBindings");

            for (Map<String, Object> cubePortBinding : cubePortBindings) {
                if (cubePortBinding.containsKey("exposedPort") && cubePortBinding.containsKey("port")) {

                    String exposedPortAndProtocol = (String) cubePortBinding.get("exposedPort");
                    int exposedPort = Integer.parseInt(exposedPortAndProtocol.substring(0,
                            exposedPortAndProtocol.indexOf("/")));
                    int port = (int) cubePortBinding.get("port");

                    binding.addPortBinding(exposedPort, port);
                }
            }

        }

        return binding;

    }
}
