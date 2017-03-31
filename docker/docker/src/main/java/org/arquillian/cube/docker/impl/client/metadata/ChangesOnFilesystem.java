package org.arquillian.cube.docker.impl.client.metadata;

import java.util.List;
import org.arquillian.cube.ChangeLog;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.metadata.CanSeeChangesOnFilesystem;

public class ChangesOnFilesystem implements CanSeeChangesOnFilesystem {

    private String cubeId;
    private DockerClientExecutor executor;

    public ChangesOnFilesystem(String cubeId, DockerClientExecutor executor) {
        this.cubeId = cubeId;
        this.executor = executor;
    }

    @Override
    public List<ChangeLog> changes() {
        return executor.inspectChangesOnContainerFilesystem(cubeId);
    }
}
