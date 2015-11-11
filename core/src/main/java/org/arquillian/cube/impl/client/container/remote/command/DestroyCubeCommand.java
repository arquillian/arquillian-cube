package org.arquillian.cube.impl.client.container.remote.command;

public class DestroyCubeCommand extends AbstractCubeCommand<String> {

    private static final long serialVersionUID = 1L;

    public DestroyCubeCommand(String cubeId) {
        super(cubeId);
    }
}
