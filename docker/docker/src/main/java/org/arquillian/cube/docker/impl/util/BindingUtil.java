package org.arquillian.cube.docker.impl.util;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.NetworkSettings;
import org.apache.commons.lang3.StringUtils;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Binding;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Logger;

public final class BindingUtil {

    public static final String PORTS_SEPARATOR = "->";

    private BindingUtil() {
    }

    public static Binding binding(final DockerClientExecutor executor, final String cubeId, final boolean dind) {
        InspectContainerResponse inspectResponse = executor.getDockerClient().inspectContainerCmd(cubeId).exec();

        String dockerIp = getDockerServerIp(executor);
        String inernalIp = dind ? getContainerIp(executor, cubeId) : null;

        Binding binding = new Binding(dockerIp, inernalIp);

        HostConfig hostConfig = inspectResponse.getHostConfig();
        if (hostConfig.getPortBindings() != null) {
            for (Entry<ExposedPort, com.github.dockerjava.api.model.Ports.Binding[]> bind : hostConfig.getPortBindings()
                .getBindings().entrySet()) {
                com.github.dockerjava.api.model.Ports.Binding[] allBindings = bind.getValue();
                for (com.github.dockerjava.api.model.Ports.Binding bindings : allBindings) {
                    binding.addPortBinding(bind.getKey().getPort(), Integer.parseInt(bindings.getHostPortSpec()));
                }
            }
        } else {
            ContainerConfig connectionConfig = inspectResponse.getConfig();
            final ExposedPort[] exposedPorts = connectionConfig.getExposedPorts();
            if (exposedPorts != null) {
                for (ExposedPort port : exposedPorts) {
                    binding.addPortBinding(port.getPort(), -1);
                }
            }
        }
        return binding;
    }

    private static String getDockerServerIp(DockerClientExecutor executor) {
        return executor.getDockerServerIp();
    }

    public static Binding binding(final String cubeId, final CubeContainer cubeConfiguration, final DockerClientExecutor executor, final boolean dind) {

        Binding binding = new Binding(dind ? getContainerIp(executor, cubeId) : executor.getDockerServerIp());

        if (cubeConfiguration.getPortBindings() != null) {
            for (PortBinding cubePortBinding : cubeConfiguration.getPortBindings()) {
                binding.addPortBinding(cubePortBinding.getExposedPort().getExposed(), cubePortBinding.getBound());
            }
        }
        return binding;
    }

    public static String getContainerIp(DockerClientExecutor dockerClientExecutor, final String id) {

        try {
            final NetworkSettings networkSettings = Optional.ofNullable(dockerClientExecutor
                .getDockerClient()
                .inspectContainerCmd(id)
                .exec().getNetworkSettings())
                .orElse(new NetworkSettings());

            //It is highly unlikely that we'll find more than one, so we'll use the first
            final Map<String, ContainerNetwork> networks = networkSettings.getNetworks();
            if (null != networks && networks.size() > 0) {
                final String ip = networks.values().iterator().next().getIpAddress();
                if (StringUtils.isNotBlank(ip)) {
                    return ip;
                }
            }
        } catch (Exception e) {
            Logger.getLogger(BindingUtil.class.getName()).warning("Falling back to localhost - Failed to get ip address for container: " + id);
        }

        return "localhost";
    }
}
