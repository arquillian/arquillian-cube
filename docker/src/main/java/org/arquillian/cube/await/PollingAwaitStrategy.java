package org.arquillian.cube.await;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.arquillian.cube.docker.DockerClientExecutor;
import org.arquillian.cube.util.Ping;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.HostConfig;
import com.github.dockerjava.api.command.InspectContainerResponse.NetworkSettings;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;

public class PollingAwaitStrategy implements AwaitStrategy {

    private static final Logger log = Logger.getLogger(PollingAwaitStrategy.class.getName());

    public static final String TAG = "polling";

    private static final int DEFAULT_POLL_ITERATIONS = 10;
    private static final int DEFAULT_SLEEP_POLL_TIME = 500;

    private DockerClientExecutor dockerClientExecutor;
    private String containerId;

    public PollingAwaitStrategy(DockerClientExecutor dockerClientExecutor, String containerId) {
        this.dockerClientExecutor = dockerClientExecutor;
        this.containerId = containerId;
    }

    @Override
    public boolean await() {
        InspectContainerResponse inspectContainer = this.dockerClientExecutor.inspectContainer(this.containerId);

        HostConfig hostConfig = inspectContainer.getHostConfig();
        Ports portBindings = hostConfig.getPortBindings();

        Map<ExposedPort, Binding> bindings = portBindings.getBindings();

        NetworkSettings networkSettings = inspectContainer.getNetworkSettings();
        // wait until container available
        for (Map.Entry<ExposedPort, Binding> binding : bindings.entrySet()) {

            log.fine(String.format("Pinging host (gateway) %s and port %s", networkSettings.getGateway(), binding
                    .getValue().getHostPort()));

            if (!Ping.ping(networkSettings.getGateway(), binding.getValue().getHostPort(), DEFAULT_POLL_ITERATIONS,
                    DEFAULT_SLEEP_POLL_TIME, TimeUnit.MILLISECONDS)) {
                return false;
            }
        }

        return true;
    }

}
