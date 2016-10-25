package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.AfterDestroy;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;


public class DockerImageController {

    private static final String TAG = ":latest";

    @Inject
    private Instance<DockerClientExecutor> dockerClientExecutorInstance;

    public void removeImage(@Observes AfterDestroy event, CubeRegistry registry, CubeDockerConfiguration configuration) {
        if (configuration.isCleanBuildImage()) {
            String cubeId = event.getCubeId();
            Cube cube = registry.getCube(cubeId);
            if(cube == null) {
                throw new IllegalArgumentException("No cube with id " + cubeId + " found in registry");
            }
            if (cube.configuration() instanceof  CubeContainer) {
                CubeContainer config = (CubeContainer) cube.configuration();

                // removing image only if it's built by cube
                if (config.getBuildImage() != null) {
                    String imageRef = event.getCubeId() + TAG;
                    DockerClientExecutor executor = dockerClientExecutorInstance.get();
                    executor.removeImage(imageRef, false);
                }
            }
        }
    }
}
