package org.arquillian.cube.impl.client.container.remote.command;

public class CreateCubeCommand extends AbstractCubeCommand<String> {

    private static final long serialVersionUID = 1L;

    public CreateCubeCommand(String cubeId) {
        super(cubeId);
    }
}
