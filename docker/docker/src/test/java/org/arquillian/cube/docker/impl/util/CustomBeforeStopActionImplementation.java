package org.arquillian.cube.docker.impl.util;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.beforeStop.BeforeStopAction;

/**
 * A CustomBeforeStopActionImplementation class used in the {{@link org.arquillian.cube.docker.impl.client.BeforeStopContainerObserverTest}}
 */
public class CustomBeforeStopActionImplementation implements BeforeStopAction {

    private DockerClientExecutor dockerClientExecutor;
    private String containerID;

    @Override
    public void doBeforeStop() {
       dockerClientExecutor.getDockerUri();
       containerID.toString();
    }
}
