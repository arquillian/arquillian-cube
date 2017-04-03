package org.arquillian.cube.impl.client.container.remote.command;

import org.arquillian.cube.TopContainer;

public class TopCommand extends AbstractCubeCommand<TopContainer> {

    private static final long serialVersionUID = 1L;

    public TopCommand(String cubeId) {
        super(cubeId);
    }
}
