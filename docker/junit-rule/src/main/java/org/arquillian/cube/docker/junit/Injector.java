package org.arquillian.cube.docker.junit;

import java.lang.reflect.Field;
import java.util.Optional;
import org.arquillian.cube.HostIpContext;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.Container;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.impl.model.LocalCubeRegistry;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.CubeLifecyleEvent;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;

public class Injector {

    public static DockerCube prepareContainer(Container container, DockerClientExecutor dockerClientExecutor, LocalCubeRegistry localCubeRegistry)
        throws NoSuchFieldException, IllegalAccessException {

        final Optional<Field>
            hostIpContextField = Reflections.findFieldByGenericType(Container.class, Instance.class, HostIpContext.class);

        if (hostIpContextField.isPresent()) {
            Reflections.injectObject(container, hostIpContextField.get(), (Instance) () -> new HostIpContext(dockerClientExecutor.getDockerServerIp()));
        }

        final Optional<Field> dockerClientExecutorField = Reflections.findFieldByGenericType(Container.class, Instance.class, DockerClientExecutor.class);

        if (dockerClientExecutorField.isPresent()) {
            Reflections.injectObject(container, dockerClientExecutorField.get(), (Instance) () -> dockerClientExecutor);
        }

        DockerCube dockerCube = new DockerCube(container.getContainerName(), container.getCubeContainer(), dockerClientExecutor);
        localCubeRegistry.addCube(dockerCube);

        final Optional<Field> cubeRegistryField = Reflections.findFieldByGenericType(Container.class, Instance.class, CubeRegistry.class);

        if (cubeRegistryField.isPresent()) {
            Reflections.injectObject(container, cubeRegistryField.get(), (Instance) () -> localCubeRegistry);
        }

        final Optional<Field> eventField = Reflections.findFieldByGenericType(DockerCube.class, Event.class, CubeLifecyleEvent.class);

        if (eventField.isPresent()) {
            Reflections.injectObject(dockerCube, eventField.get(), (Event) o -> {
            });
        }

        return dockerCube;

    }

}
