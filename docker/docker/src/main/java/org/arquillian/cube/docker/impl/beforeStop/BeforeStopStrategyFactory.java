package org.arquillian.cube.docker.impl.beforeStop;

import java.util.logging.Logger;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.beforeStop.BeforeStopStrategy;

public class BeforeStopStrategyFactory {

    private static final Logger log = Logger.getLogger(BeforeStopStrategyFactory.class.getName());

    private BeforeStopStrategyFactory() {
        super();
    }

    public static final BeforeStopStrategy create(DockerClientExecutor dockerClientExecutor, String containerId, String beforeStopStrategy) {

        String strategy = beforeStopStrategy.toLowerCase();
        return new CustomBeforeStopStrategyInstantiator(containerId, dockerClientExecutor, strategy);
    }
}
