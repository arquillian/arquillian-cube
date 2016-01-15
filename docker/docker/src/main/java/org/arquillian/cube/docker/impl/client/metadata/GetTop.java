package org.arquillian.cube.docker.impl.client.metadata;

import org.arquillian.cube.TopContainer;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.metadata.CanSeeTop;

public class GetTop implements CanSeeTop {

    private String cubeId;
    private DockerClientExecutor executor;

    public GetTop(String cubeId, DockerClientExecutor executor) {
        this.cubeId = cubeId;
        this.executor = executor;
    }

    @Override
    public TopContainer top() {
        return executor.top(cubeId);
    }
}
