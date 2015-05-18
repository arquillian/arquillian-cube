package org.arquillian.cube.impl.client;

import java.io.OutputStream;
import java.util.List;

import org.arquillian.cube.ChangeLog;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.CubeID;
import org.arquillian.cube.TopContainer;
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
    public void create(CubeID cubeId) {
        create(cubeId.get());
    }

    @Override
    public void start(CubeID cubeId) {
        start(cubeId.get());
    }

    @Override
    public void stop(CubeID cubeId) {
        stop(cubeId.get());
    }

    @Override
    public void destroy(CubeID cubeId) {
        destroy(cubeId.get());
    }


    @Override
    public void create(String cubeId) {
        Cube cube = cubeRegistry.get().getCube(cubeId);

        Validate.notNull(cube, "Cube with id '" + cubeId + "' to create does not exist.");

        controlEvent.fire(new CreateCube(cubeId));
    }

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
    public void destroy(String cubeId) {
        Cube cube = cubeRegistry.get().getCube(cubeId);

        Validate.notNull(cube, "Cube with id '" + cubeId + "' to destroy does not exist.");

        controlEvent.fire(new DestroyCube(cubeId));
    }

    @Override
    public void copyFileDirectoryFromContainer(String cubeId, String from,
            String to) {
        Cube cube = cubeRegistry.get().getCube(cubeId);

        Validate.notNull(cube, "Cube with id '" + cubeId + "' to execute copy file command does not exist.");

        cube.copyFileDirectoryFromContainer(cubeId, from, to);
    }

    @Override
    public void copyFileDirectoryFromContainer(CubeID cubeId, String from,
            String to) {
        copyFileDirectoryFromContainer(cubeId.get(), from, to);
    }

    @Override
    public List<ChangeLog> changesOnFilesystem(String cubeId) {
        Cube cube = cubeRegistry.get().getCube(cubeId);

        Validate.notNull(cube, "Cube with id '" + cubeId + "' to get changes command does not exist.");

        return cube.changesOnFilesystem(cubeId);
    }

    @Override
    public List<ChangeLog> changesOnFilesystem(CubeID cubeId) {
        return changesOnFilesystem(cubeId.get());
    }

    @Override
    public void copyLog(String cubeId, boolean follow,
            boolean stdout, boolean stderr, boolean timestamps, int tail,
            OutputStream outputStream) {
        Cube cube = cubeRegistry.get().getCube(cubeId);

        Validate.notNull(cube, "Cube with id '" + cubeId + "' to get logs command does not exist.");

        cube.copyLog(cubeId, follow, stdout, stderr, timestamps, tail, outputStream);
    }

    @Override
    public void copyLog(CubeID containerId, boolean follow,
            boolean stdout, boolean stderr, boolean timestamps, int tail,
            OutputStream outputStream) {
        copyLog(containerId.get(), follow, stdout, stderr, timestamps, tail, outputStream);
    }

    @Override
    public TopContainer top(String cubeId) {
        Cube cube = cubeRegistry.get().getCube(cubeId);

        Validate.notNull(cube, "Cube with id '" + cubeId + "' to get top command does not exist.");

        return cube.top(cubeId);
    }

    @Override
    public TopContainer top(CubeID cubeId) {
        return top(cubeId.get());
    }

}
