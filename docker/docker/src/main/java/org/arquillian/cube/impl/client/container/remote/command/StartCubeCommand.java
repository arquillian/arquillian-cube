package org.arquillian.cube.impl.client.container.remote.command;

public class StartCubeCommand extends AbstractCubeCommand<String> {

    private static final long serialVersionUID = 1L;

    public StartCubeCommand(String cubeId) {
        super(cubeId);
    }
}
