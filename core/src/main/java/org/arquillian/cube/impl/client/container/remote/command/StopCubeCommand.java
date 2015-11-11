package org.arquillian.cube.impl.client.container.remote.command;

public class StopCubeCommand extends AbstractCubeCommand<String> {

    private static final long serialVersionUID = 1L;

    public StopCubeCommand(String cubeId) {
        super(cubeId);
    }
}
