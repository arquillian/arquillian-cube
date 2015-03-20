package org.arquillian.cube.impl.client.container.remote.command;

import java.util.List;

import org.arquillian.cube.ChangeLog;

public class ChangesOnFilesystemCommand extends
        AbstractCubeCommand<List<ChangeLog>> {

    private static final long serialVersionUID = 1L;

    public ChangesOnFilesystemCommand(String cubeId) {
        super(cubeId);
    }

}
