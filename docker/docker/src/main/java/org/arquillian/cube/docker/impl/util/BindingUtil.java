package org.arquillian.cube.docker.impl.util;

import java.util.Map.Entry;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Binding;

public final class BindingUtil {

    public static final String PORTS_SEPARATOR = "->";

    private BindingUtil() {
    }

    public static Binding binding(final DockerClientExecutor executor, final String cubeId) {

        final String dockerIp = executor.getDockerServerIp();
        final String internal = executor.isDockerInsideDockerResolution() ? dockerIp : executor.getDockerUri().getHost();

        Binding binding = new Binding(dockerIp, internal);

        final InspectContainerResponse inspectResponse = executor.getDockerClient().inspectContainerCmd(cubeId).exec();
        final HostConfig hostConfig = inspectResponse.getHostConfig();

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

    public static Binding binding(final CubeContainer cubeConfiguration, final DockerClientExecutor executor) {

        Binding binding = new Binding(executor.isDockerInsideDockerResolution() ? executor.getDockerServerIp() : executor.getDockerUri().getHost());

        if (cubeConfiguration.getPortBindings() != null) {
            for (PortBinding cubePortBinding : cubeConfiguration.getPortBindings()) {
                binding.addPortBinding(cubePortBinding.getExposedPort().getExposed(), cubePortBinding.getBound());
            }
        }
        return binding;
    }
}
