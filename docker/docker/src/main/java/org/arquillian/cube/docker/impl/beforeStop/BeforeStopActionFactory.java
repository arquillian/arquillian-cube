package org.arquillian.cube.docker.impl.beforeStop;

import java.util.logging.Logger;

import org.arquillian.cube.docker.impl.client.config.CustomBeforeStopAction;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.beforeStop.BeforeStopStrategy;

public class BeforeStopActionFactory {

    private static final Logger log = Logger.getLogger(BeforeStopActionFactory.class.getName());

    private BeforeStopActionFactory() {
        super();
    }

    public static final BeforeStopStrategy create(DockerClientExecutor dockerClientExecutor, String containerId, CustomBeforeStopAction beforeStopStrategy) {

        String strategy = beforeStopStrategy.getStrategy().toLowerCase();
        return new CustomBeforeStopActionInstantiator(containerId, dockerClientExecutor, strategy);
    }
}
