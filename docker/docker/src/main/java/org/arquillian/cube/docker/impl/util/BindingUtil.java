package org.arquillian.cube.docker.impl.util;

import java.util.Map.Entry;

import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Binding;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.NetworkSettings;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;

public final class BindingUtil {

    public static final String PORTS_SEPARATOR = "->";

    private BindingUtil() {
    }

    public static Binding binding(DockerClientExecutor executor, String cubeId) {
        InspectContainerResponse inspectResponse = executor.getDockerClient().inspectContainerCmd( cubeId ).exec();

        String dockerIp = getDockerServerIp(executor);
        String inernalIp = null;
        NetworkSettings networkSettings = inspectResponse.getNetworkSettings();
        if(networkSettings != null) {
            inernalIp = networkSettings.getIpAddress();
        }

        Binding binding = new Binding(dockerIp, inernalIp);

        HostConfig hostConfig = inspectResponse.getHostConfig();
        if(hostConfig.getPortBindings() != null) {
            for (Entry<ExposedPort, com.github.dockerjava.api.model.Ports.Binding[]> bind : hostConfig.getPortBindings()
                    .getBindings().entrySet()) {
                com.github.dockerjava.api.model.Ports.Binding[] allBindings = bind.getValue();
                for (com.github.dockerjava.api.model.Ports.Binding bindings : allBindings) {
                    binding.addPortBinding(bind.getKey().getPort(), Integer.parseInt(bindings.getHostPortSpec()));
                }
            }
        } else {
            ContainerConfig connectionConfig = inspectResponse.getConfig();
            for(ExposedPort port : connectionConfig.getExposedPorts()) {
                binding.addPortBinding(port.getPort(), -1);
            }
        }
        return binding;
    }

    private static String getDockerServerIp(DockerClientExecutor executor) {
        return executor.getDockerServerIp();
    }

    public static Binding binding(CubeContainer cubeConfiguration, DockerClientExecutor executor) {

        Binding binding = new Binding(executor.getDockerServerIp());

        if (cubeConfiguration.getPortBindings() != null) {
            for (PortBinding cubePortBinding : cubeConfiguration.getPortBindings()) {
                binding.addPortBinding(cubePortBinding.getExposedPort().getExposed(), cubePortBinding.getBound());
            }
        }
        return binding;
    }
}
