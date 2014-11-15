package org.arquillian.cube.impl.client.container.remote;

import org.arquillian.cube.CubeController;
import org.arquillian.cube.CubeID;
import org.arquillian.cube.impl.client.container.remote.command.CreateCubeCommand;
import org.arquillian.cube.impl.client.container.remote.command.DestroyCubeCommand;
import org.arquillian.cube.impl.client.container.remote.command.StartCubeCommand;
import org.arquillian.cube.impl.client.container.remote.command.StopCubeCommand;
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

    private CommandService getCommandService()
    {
       ServiceLoader loader = serviceLoader.get();
       if(loader == null)
       {
          throw new IllegalStateException("No " + ServiceLoader.class.getName() + " found in context");
       }
       CommandService service = loader.onlyOne(CommandService.class);
       if(service == null)
       {
          throw new IllegalStateException("No " + CommandService.class.getName() + " found in context");
       }
       return service;
    }
}
