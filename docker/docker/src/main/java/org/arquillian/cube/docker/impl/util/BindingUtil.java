package org.arquillian.cube.docker.impl.util;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Binding;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;

public final class BindingUtil {

    public static final String PORTS_SEPARATOR = "->";
    private static final String NO_GATEWAY = null;

    private BindingUtil() {
    }

    public static Binding binding(DockerClientExecutor executor, String cubeId) {
        InspectContainerResponse inspectResponse = executor.getDockerClient().inspectContainerCmd( cubeId ).exec();

        HostConfig hostConfig = inspectResponse.getHostConfig();

        String dockerIp = getDockerServerIp(executor);
        Binding binding = new Binding(dockerIp);

        for (Entry<ExposedPort, com.github.dockerjava.api.model.Ports.Binding[]> bind : hostConfig.getPortBindings()
                .getBindings().entrySet()) {
            com.github.dockerjava.api.model.Ports.Binding[] allBindings = bind.getValue();
            for (com.github.dockerjava.api.model.Ports.Binding bindings : allBindings) {
                binding.addPortBinding(bind.getKey().getPort(), bindings.getHostPort());
            }
        }
        return binding;
    }

    private static String getDockerServerIp(DockerClientExecutor executor) {
        return executor.getDockerServerIp();
    }

    public static Binding binding(Map<String, Object> cubeConfiguration) {

        Binding binding = new Binding(NO_GATEWAY);

        if (cubeConfiguration.containsKey("portBindings")) {
            @SuppressWarnings("unchecked")
            List<String> cubePortBindings = (List<String>) cubeConfiguration.get("portBindings");

            for (String cubePortBinding : cubePortBindings) {

                String[] elements = cubePortBinding.split(PORTS_SEPARATOR);

                if (elements.length == 1) {
                	int positionOfProtocolSeparator = elements[0].indexOf("/");
                    String bindingPortValue = elements[0];
                    if(positionOfProtocolSeparator > -1) {
                        //means that the protocol part is also set. 
                        bindingPortValue = elements[0].substring(0, positionOfProtocolSeparator);
                    }
                    int exposedPort = Integer.parseInt(bindingPortValue);
                    binding.addPortBinding(exposedPort, exposedPort);
                } else {
                    if (elements.length == 2) {
                        int exposedPort = Integer.parseInt(elements[1].substring(0, elements[1].indexOf("/")));
                        int port = Integer.parseInt(elements[0]);

                        binding.addPortBinding(exposedPort, port);
                    }
                }

            }

        }

        return binding;

    }
}
