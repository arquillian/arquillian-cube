package org.arquillian.cube.impl.client.container;

import java.util.List;

import org.arquillian.cube.ChangeLog;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.TopContainer;
import org.arquillian.cube.impl.client.container.remote.command.ChangesOnFilesystemCommand;
import org.arquillian.cube.impl.client.container.remote.command.CopyFileDirectoryCommand;
import org.arquillian.cube.impl.client.container.remote.command.CreateCubeCommand;
import org.arquillian.cube.impl.client.container.remote.command.CubeIDCommand;
import org.arquillian.cube.impl.client.container.remote.command.DestroyCubeCommand;
import org.arquillian.cube.impl.client.container.remote.command.StartCubeCommand;
import org.arquillian.cube.impl.client.container.remote.command.StopCubeCommand;
import org.arquillian.cube.impl.client.container.remote.command.TopCommand;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeRemoteCommandObserver {

    private static final String SUCCESS = "SUCCESS";

    @Inject
    private Instance<Container> containerInst;

    @Inject
    private Instance<CubeRegistry> cubeRegistryInst;

    public void create(@Observes CreateCubeCommand command, CubeController controller) {
        controller.create(command.getCubeId());
        command.setResult(SUCCESS);
    }

    public void start(@Observes StartCubeCommand command, CubeController controller) {
        controller.start(command.getCubeId());
        command.setResult(SUCCESS);
    }

    public void stop(@Observes StopCubeCommand command, CubeController controller) {
        controller.stop(command.getCubeId());
        command.setResult(SUCCESS);
    }

    public void destroy(@Observes DestroyCubeCommand command, CubeController controller) {
        controller.destroy(command.getCubeId());
        command.setResult(SUCCESS);
    }

    public void copyFileDirectory(@Observes CopyFileDirectoryCommand command, CubeController controller) {
        controller.copyFileDirectoryFromContainer(command.getCubeId(), command.getFrom(), command.getTo());
        command.setResult(SUCCESS);
    }

    public void changesOnFilesystem(@Observes ChangesOnFilesystemCommand command, CubeController controller) {
        List<ChangeLog> changesOnFilesystem = controller.changesOnFilesystem(command.getCubeId());
        command.setResult(changesOnFilesystem);
    }

    public void top(@Observes TopCommand command, CubeController controller) {
        TopContainer top = controller.top(command.getCubeId());
        command.setResult(top);
    }

    public void getCubeID(@Observes CubeIDCommand command) {
        Container container = containerInst.get();
        if(container == null) {
            throw new IllegalStateException("No Container found in context, can't perform CubeID injection");
        }
        CubeRegistry cubeRegistry = cubeRegistryInst.get();
        if(cubeRegistry == null) {
            throw new IllegalStateException("No CubeRegistry found in context, can't perform CubeID injection");
        }
        Cube cube = cubeRegistry.getCube(container.getName());
        if(cube == null) {
            throw new IllegalStateException("No Cube found mapped to current Container: " + container.getName());
        }
        command.setResult(cube.getId());
    }
}
