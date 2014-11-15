package org.arquillian.cube.impl.client.container.remote.command;

public abstract class AbstractCubeCommand extends AbstractCommand<String> {

    private static final long serialVersionUID = 1L;

    private String cubeId;

    public AbstractCubeCommand(String cubeId) {
        this.cubeId = cubeId;
    }

    public String getCubeId() {
        return cubeId;
    }
}
