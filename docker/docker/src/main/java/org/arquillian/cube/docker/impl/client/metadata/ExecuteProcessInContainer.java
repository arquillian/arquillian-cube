package org.arquillian.cube.docker.impl.client.metadata;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.metadata.CanExecuteProcessInContainer;

public class ExecuteProcessInContainer implements CanExecuteProcessInContainer {

    private String cubeId;
    private DockerClientExecutor executor;

    public ExecuteProcessInContainer(String cubeId, DockerClientExecutor executor) {
        this.cubeId = cubeId;
        this.executor = executor;
    }

    @Override
    public ExecResult exec(String... command) {
        final DockerClientExecutor.ExecInspection execInspection = this.executor.execStartVerbose(cubeId, command);
        return new ExecResult(execInspection.getOutput(),
            execInspection.getInspectExecResponse().isRunning(),
            execInspection.getInspectExecResponse().getExitCode());
    }
}
