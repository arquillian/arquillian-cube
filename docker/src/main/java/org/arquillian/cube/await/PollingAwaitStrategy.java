package org.arquillian.cube.await;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    public static final String TAG = "polling";
    
    private static final int DEFAULT_POLL_ITERATIONS = 10;
    private static final int DEFAULT_SLEEP_POLL_TIME = 500;
    
    private DockerClientExecutor dockerClientExecutor;
    private CreateContainerResponse createContainer;
    
    public PollingAwaitStrategy(DockerClientExecutor dockerClientExecutor, CreateContainerResponse createContainer) {
        this.dockerClientExecutor = dockerClientExecutor;
        this.createContainer = createContainer;
    }
    
    @Override
    public boolean await() {
        InspectContainerResponse inspectContainer = this.dockerClientExecutor.inspectContainer(this.createContainer);

        HostConfig hostConfig = inspectContainer.getHostConfig();
        Ports portBindings = hostConfig.getPortBindings();

        Map<ExposedPort, Binding> bindings = portBindings.getBindings();

        NetworkSettings networkSettings = inspectContainer.getNetworkSettings();
        // wait until container available
        for (Map.Entry<ExposedPort, Binding> binding : bindings.entrySet()) {
            if(!Ping.ping(networkSettings.getGateway(), binding.getValue().getHostPort(), DEFAULT_POLL_ITERATIONS, DEFAULT_SLEEP_POLL_TIME,
                    TimeUnit.MILLISECONDS)) {
                return false;
            }
        }
        
        return true;
    }

}
