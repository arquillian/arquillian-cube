package org.arquillian.cube.docker.impl.afterStart;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.model.CubeId;
import org.arquillian.cube.spi.afterStart.AfterStartAction;

/**
 * A test class used in the {{@link org.arquillian.cube.docker.impl.client.AfterStartContainerObserverTest}} and in
 * {{@link AfterStartActionTest}}
 */
public class CustomAfterStartActionImpl implements AfterStartAction {

    private DockerClientExecutor dockerClientExecutor;
    private CubeId containerID;

    @Override
    public void doAfterStart() {
        dockerClientExecutor.getDockerUri();
        containerID.toString();
    }
}
