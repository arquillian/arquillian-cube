package org.arquillian.cube.impl.client.container;

import org.arquillian.cube.CubeController;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.CreateCube;
import org.arquillian.cube.spi.event.CubeControlEvent;
import org.arquillian.cube.spi.event.DestroyCube;
import org.arquillian.cube.spi.event.StartCube;
import org.arquillian.cube.spi.event.StopCube;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.spi.Validate;

/**
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
public class ClientCubeController implements CubeController {

    @Inject
    private Instance<CubeRegistry> cubeRegistry;

    @Inject
    private Event<CubeControlEvent> controlEvent;

    @Override
    public void start(String cubeId) {
        Cube cube = cubeRegistry.get().getCube(cubeId);

        Validate.notNull(cube, "Cube with id '" + cubeId + "' to start does not exist.");

        controlEvent.fire(new StartCube(cubeId));
    }

    @Override
    public void stop(String cubeId) {
        Cube cube = cubeRegistry.get().getCube(cubeId);

        Validate.notNull(cube, "Cube with id '" + cubeId + "' to stop does not exist.");

        controlEvent.fire(new StopCube(cubeId));

    }

    @Override
    public void create(String cubeId) {
        Cube cube = cubeRegistry.get().getCube(cubeId);

        Validate.notNull(cube, "Cube with id '" + cubeId + "' to create does not exist.");

        controlEvent.fire(new CreateCube(cubeId));
    }

    @Override
    public void destroy(String cubeId) {
        Cube cube = cubeRegistry.get().getCube(cubeId);

        Validate.notNull(cube, "Cube with id '" + cubeId + "' to destroy does not exist.");

        controlEvent.fire(new DestroyCube(cubeId));
    }

}
