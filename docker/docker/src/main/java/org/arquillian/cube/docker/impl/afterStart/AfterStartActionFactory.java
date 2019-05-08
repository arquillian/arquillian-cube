package org.arquillian.cube.docker.impl.afterStart;

import org.arquillian.cube.docker.impl.client.config.CustomAfterStartAction;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.model.CubeId;
import org.arquillian.cube.spi.afterStart.AfterStartAction;

public class AfterStartActionFactory {

    public static final AfterStartAction create(DockerClientExecutor dockerClientExecutor, CubeId containerId, CustomAfterStartAction afterStartStrategy) {
        return new CustomAfterStartActionInstantiator(containerId, dockerClientExecutor, afterStartStrategy);
    }
}
