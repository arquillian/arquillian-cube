package org.arquillian.cube.docker.impl.client.containerobject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

import org.arquillian.cube.ContainerObjectConfiguration;
import org.arquillian.cube.ContainerObjectFactory;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.arquillian.test.spi.TestEnricher;

/**
 * An implementation of {@link ContainerObjectFactory} for Docker images.
 *
 * @see DockerContainerObjectBuilder
 *
 * @author <a href="mailto:rivasdiaz@gmail.com">Ramon Rivas</a>
 */
public class DockerContainerObjectFactory implements ContainerObjectFactory {

    private static final Logger logger = Logger.getLogger(DockerContainerObjectFactory.class.getName());

    @Inject Instance<ServiceLoader> serviceLoaderInstance;
    @Inject Instance<DockerClientExecutor> dockerClientExecutorInstance;
    @Inject Instance<CubeRegistry> cubeRegistryInstance;
    @Inject Instance<CubeController> cubeControllerInstance;
    @Inject Instance<Injector> injectorInstance;

    @Override
    public <T> T createContainerObject(Class<T> containerObjectClass) {
        return createContainerObject(containerObjectClass, CubeContainerObjectConfiguration.empty(),null);
    }

    @Override
    public <T> T createContainerObject(Class<T> containerObjectClass, ContainerObjectConfiguration configuration) {
        return createContainerObject(containerObjectClass, configuration,null);
    }

    public <T> T createContainerObject(Class<T> containerObjectClass, ContainerObjectConfiguration configuration, Object containerObjectContainer) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }
        try {
            return new DockerContainerObjectBuilder<T>(dockerClientExecutorInstance.get(), cubeControllerInstance.get(), cubeRegistryInstance.get())
                    .withEnrichers(serviceLoaderInstance.get().all(TestEnricher.class))
                    .withContainerObjectClass(containerObjectClass)
                    .withContainerObjectConfiguration(configuration)
                    .withContainerObjectContainer(containerObjectContainer)
                    .onCubeCreated(this::onCubeCreated)
                    .build();
        } catch (IllegalAccessException | IOException | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private void onCubeCreated(DockerCube cube) {
        injectorInstance.get().inject(cube);
        cubeRegistryInstance.get().addCube(cube);
    }
}
