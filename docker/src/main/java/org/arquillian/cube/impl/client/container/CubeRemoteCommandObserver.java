package org.arquillian.cube.impl.client.container;

import org.arquillian.cube.CubeController;
import org.arquillian.cube.impl.client.container.remote.command.CreateCubeCommand;
import org.arquillian.cube.impl.client.container.remote.command.DestroyCubeCommand;
import org.arquillian.cube.impl.client.container.remote.command.StartCubeCommand;
import org.arquillian.cube.impl.client.container.remote.command.StopCubeCommand;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeRemoteCommandObserver {

    private static final String SUCCESS = "SUCCESS";

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
}
