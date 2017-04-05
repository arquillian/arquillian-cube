package org.arquillian.cube.docker.impl.beforeStop;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.beforeStop.BeforeStopAction;

/**
 * A test class used in the {{@link org.arquillian.cube.docker.impl.client.BeforeStopContainerObserverTest}} and in
 * {{@link BeforeStopActionTest}}
 */
public class CustomBeforeStopActionImpl implements BeforeStopAction {

    private DockerClientExecutor dockerClientExecutor;
    private String containerID;

    @Override
    public void doBeforeStop() {
        dockerClientExecutor.getDockerUri();
        containerID.toString();
    }
}
