package org.arquillian.cube.docker.impl.client.containerobject;

import org.arquillian.cube.CubeController;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.IsContainerObject;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;

import java.util.List;
import java.util.logging.Logger;

public class AfterClassContainerObjectObserver {

    private static final Logger logger = Logger.getLogger(AfterClassContainerObjectObserver.class.getName());

    @Inject Instance<CubeRegistry> cubeRegistryInstance;
    @Inject Instance<CubeController> cubeControllerInstance;

    public void stopContainerObjects(@Observes AfterClass afterClass) {

        final CubeController cubeController = cubeControllerInstance.get();
        final List<Cube<?>> byMetadata = cubeRegistryInstance.get().getByMetadata(IsContainerObject.class);
        for (Cube<?> cube : byMetadata) {
            // To support fork tests
            final Class<?> testJavaClass = afterClass.getTestClass().getJavaClass();
            if (testJavaClass.equals(cube.getMetadata(IsContainerObject.class).getTestClass())) {
                logger.fine(String.format("Stopping Container Object %s", cube.getId()));
                cubeController.stop(cube.getId());
                cubeController.destroy(cube.getId());
                cubeRegistryInstance.get().removeCube(cube.getId());
            }
        }
    }

}
