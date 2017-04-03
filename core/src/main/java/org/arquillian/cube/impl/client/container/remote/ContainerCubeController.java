package org.arquillian.cube.impl.client.container.remote;

import java.io.OutputStream;
import java.util.List;
import org.arquillian.cube.ChangeLog;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.CubeID;
import org.arquillian.cube.TopContainer;
import org.arquillian.cube.impl.client.container.remote.command.ChangesOnFilesystemCommand;
import org.arquillian.cube.impl.client.container.remote.command.CopyFileDirectoryCommand;
import org.arquillian.cube.impl.client.container.remote.command.CreateCubeCommand;
import org.arquillian.cube.impl.client.container.remote.command.DestroyCubeCommand;
import org.arquillian.cube.impl.client.container.remote.command.StartCubeCommand;
import org.arquillian.cube.impl.client.container.remote.command.StopCubeCommand;
import org.arquillian.cube.impl.client.container.remote.command.TopCommand;
import org.jboss.arquillian.container.test.spi.command.CommandService;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.spi.ServiceLoader;

public class ContainerCubeController implements CubeController {

    @Inject
    private Instance<ServiceLoader> serviceLoader;

    @Override
    public void create(CubeID cubeId) {
        this.create(cubeId.get());
    }

    @Override
    public void start(CubeID cubeId) {
        this.start(cubeId.get());
    }

    @Override
    public void stop(CubeID cubeId) {
        this.stop(cubeId.get());
    }

    @Override
    public void destroy(CubeID cubeId) {
        this.destroy(cubeId.get());
    }

    @Override
    public void create(String cubeId) {
        getCommandService().execute(new CreateCubeCommand(cubeId));
    }

    @Override
    public void start(String cubeId) {
        getCommandService().execute(new StartCubeCommand(cubeId));
    }

    @Override
    public void stop(String cubeId) {
        getCommandService().execute(new StopCubeCommand(cubeId));
    }

    @Override
    public void destroy(String cubeId) {
        getCommandService().execute(new DestroyCubeCommand(cubeId));
    }

    @Override
    public void copyFileDirectoryFromContainer(String cubeId, String from,
        String to) {
        getCommandService().execute(new CopyFileDirectoryCommand(cubeId, from, to));
    }

    @Override
    public void copyFileDirectoryFromContainer(CubeID cubeId, String from,
        String to) {
        copyFileDirectoryFromContainer(cubeId.get(), from, to);
    }

    @Override
    public List<ChangeLog> changesOnFilesystem(String cubeId) {
        return getCommandService().execute(new ChangesOnFilesystemCommand(cubeId));
    }

    @Override
    public List<ChangeLog> changesOnFilesystem(CubeID cubeId) {
        return changesOnFilesystem(cubeId.get());
    }

    @Override
    public void copyLog(String containerId, boolean follow,
        boolean stdout, boolean stderr, boolean timestamps, int tail,
        OutputStream outputStream) {
        throw new UnsupportedOperationException(
            "This operation is only supported for tests running in client mode. https://docs.jboss.org/author/display/ARQ/Test+run+modes");
    }

    @Override
    public void copyLog(CubeID containerId, boolean follow,
        boolean stdout, boolean stderr, boolean timestamps, int tail,
        OutputStream outputStream) {
        copyLog(containerId.get(), follow, stdout, stderr, timestamps, tail, outputStream);
    }

    @Override
    public TopContainer top(String cubeId) {
        return getCommandService().execute(new TopCommand(cubeId));
    }

    @Override
    public TopContainer top(CubeID cubeId) {
        return top(cubeId.get());
    }

    private CommandService getCommandService() {
        ServiceLoader loader = serviceLoader.get();
        if (loader == null) {
            throw new IllegalStateException("No " + ServiceLoader.class.getName() + " found in context");
        }
        CommandService service = loader.onlyOne(CommandService.class);
        if (service == null) {
            throw new IllegalStateException("No " + CommandService.class.getName() + " found in context");
        }
        return service;
    }
}
