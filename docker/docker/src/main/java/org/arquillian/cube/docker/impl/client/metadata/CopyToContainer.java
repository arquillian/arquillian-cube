package org.arquillian.cube.docker.impl.client.metadata;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.metadata.CanCopyToContainer;

public class CopyToContainer implements CanCopyToContainer {

    private String cubeId;
    private DockerClientExecutor executor;


    public CopyToContainer(String cubeId, DockerClientExecutor executor) {
        this.cubeId = cubeId;
        this.executor = executor;
    }

    @Override
    public void copyDirectory(String from, String to) {
        executor.copyStreamToContainer(cubeId, from, to);
    }

    @Override
    public void copyDirectory(String from) {
        executor.copyStreamToContainer(cubeId, from);
    }
}
