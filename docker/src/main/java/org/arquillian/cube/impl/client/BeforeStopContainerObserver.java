package org.arquillian.cube.impl.client;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.arquillian.cube.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.BeforeStop;
import org.jboss.arquillian.core.api.annotation.Observes;

public class BeforeStopContainerObserver {

    private static final String BEFORE_STOP = "beforeStop";
    private static final String COPY = "copy";
    private static final String LOG = "log";

    @SuppressWarnings("unchecked")
    public void processCommands(@Observes BeforeStop beforeStop, CubeRegistry cubeRegistry,
            DockerClientExecutor dockerClientExecutor) throws IOException {

        Cube cube = cubeRegistry.getCube(beforeStop.getCubeId());
        Map<String, Object> configuration = cube.configuration();

        if (configuration.containsKey(BEFORE_STOP)) {
            List<Map<String, Object>> beforeStopConfiguration = (List<Map<String, Object>>) configuration
                    .get(BEFORE_STOP);

            for (Map<String, Object> map : beforeStopConfiguration) {
                if (map.containsKey(COPY)) {
                    Map<String, Object> copyConfiguration = (Map<String, Object>) map.get(COPY);
                    dockerClientExecutor.copyFromContainer(beforeStop.getCubeId(), copyConfiguration);
                } else {
                    if (map.containsKey(LOG)) {
                        Map<String, Object> logConfiguration = (Map<String, Object>) map.get(LOG);
                        dockerClientExecutor.copyLog(beforeStop.getCubeId(), logConfiguration);
                    }
                }
            }
        }
    }
}
