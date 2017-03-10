package org.arquillian.cube.docker.impl.client.containerobject;

import org.arquillian.cube.CubeController;
import org.arquillian.cube.containerobject.ConnectionMode;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.IsContainerObject;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.After;

import java.util.List;
import java.util.logging.Logger;

/**
 * Observer that stops and removes started container objects at the end of test method execution.
 */
public class AfterContainerObjectObserver {

    private static final Logger logger = Logger.getLogger(AfterContainerObjectObserver.class.getName());

    @Inject
    Instance<CubeRegistry> cubeRegistryInstance;
    @Inject Instance<CubeController> cubeControllerInstance;

    public void stopContainerObjects(@Observes(precedence = 100) After afterClass) {

        final CubeController cubeController = cubeControllerInstance.get();
        final List<Cube<?>> byMetadata = cubeRegistryInstance.get().getByMetadata(IsContainerObject.class);
        byMetadata.stream()
                .filter( cube -> {
                    // To support fork tests
                    final Class<?> testJavaClass = afterClass.getTestClass().getJavaClass();
                    return testJavaClass.equals(cube.getMetadata(IsContainerObject.class).getTestClass());
                })
                .filter(cube -> cube.getMetadata(IsContainerObject.class).getConnectionMode() == ConnectionMode.START_AND_STOP_AROUND_METHOD)
                .forEach(cube -> {
                    logger.fine(String.format("Stopping Container Object %s", cube.getId()));
                    cubeController.stop(cube.getId());
                    cubeController.destroy(cube.getId());
                    cubeRegistryInstance.get().removeCube(cube.getId());
                });
    }

}
