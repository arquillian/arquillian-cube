package org.arquillian.cube.docker.impl.client.containerobject;

import java.util.List;
import java.util.logging.Logger;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.containerobject.ConnectionMode;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.IsContainerObject;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;

/**
 * Observer that stops and removes started container objects at the end of class execution.
 */
public class AfterClassContainerObjectObserver {

    private static final Logger logger = Logger.getLogger(AfterClassContainerObjectObserver.class.getName());

    @Inject Instance<CubeRegistry> cubeRegistryInstance;
    @Inject Instance<CubeController> cubeControllerInstance;

    public void stopContainerObjects(@Observes(precedence = 100) After afterClass) {

        final ConnectionMode connectionMode = ConnectionMode.START_AND_STOP_AROUND_METHOD;
        final TestClass testClass = afterClass.getTestClass();

        stopAndDestroyCubes(connectionMode, testClass);
    }

    public void stopContainerObjects(@Observes(precedence = 100) AfterClass afterClass) {

        final ConnectionMode connectionMode = ConnectionMode.START_AND_STOP_AROUND_CLASS;
        final TestClass testClass = afterClass.getTestClass();

        stopAndDestroyCubes(connectionMode, testClass);
    }

    private void stopAndDestroyCubes(ConnectionMode connectionMode, TestClass testClass) {
        final CubeController cubeController = cubeControllerInstance.get();
        final List<Cube<?>> byMetadata = cubeRegistryInstance.get().getByMetadata(IsContainerObject.class);
        byMetadata.stream()
            .filter(cube -> {
                // To support fork tests
                final Class<?> testJavaClass = testClass.getJavaClass();
                return testJavaClass.equals(cube.getMetadata(IsContainerObject.class).getTestClass());
            })
            .filter(cube -> cube.getMetadata(IsContainerObject.class).getConnectionMode() == connectionMode)
            .forEach(cube -> {
                logger.fine(String.format("Stopping Container Object %s", cube.getId()));
                cubeController.stop(cube.getId());
                cubeController.destroy(cube.getId());
                cubeRegistryInstance.get().removeCube(cube.getId());
            });
    }
}
