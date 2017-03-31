package org.arquillian.cube.impl.client.container.remote;

import java.lang.annotation.Annotation;
import org.arquillian.cube.CubeID;
import org.arquillian.cube.impl.client.container.remote.command.CubeIDCommand;
import org.jboss.arquillian.container.test.spi.command.CommandService;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public class ContainerCubeIDProvider implements ResourceProvider {

    @Inject
    private Instance<ServiceLoader> serviceLoader;

    @Override
    public boolean canProvide(Class<?> type) {
        return CubeID.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource,
        Annotation... qualifiers) {
        return getCubeID();
    }

    private CubeID getCubeID() {
        String cubeId = getCommandService().execute(new CubeIDCommand());
        return new CubeID(cubeId);
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
