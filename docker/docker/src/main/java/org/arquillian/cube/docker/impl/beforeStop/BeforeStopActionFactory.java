package org.arquillian.cube.docker.impl.beforeStop;

import java.util.logging.Logger;

import org.arquillian.cube.docker.impl.client.config.CustomBeforeStopAction;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.model.CubeId;
import org.arquillian.cube.spi.beforeStop.BeforeStopAction;

public class BeforeStopActionFactory {

    private static final Logger log = Logger.getLogger(BeforeStopActionFactory.class.getName());

    private BeforeStopActionFactory() {
        super();
    }

    public static final BeforeStopAction create(DockerClientExecutor dockerClientExecutor, CubeId containerId, CustomBeforeStopAction beforeStopStrategy) {

        return new CustomBeforeStopActionInstantiator(containerId, dockerClientExecutor, beforeStopStrategy);
    }
}
