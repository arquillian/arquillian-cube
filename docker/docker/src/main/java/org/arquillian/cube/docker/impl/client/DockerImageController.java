package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.AfterDestroy;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import static org.arquillian.cube.impl.client.CubeLifecycleController.validateAndGet;


public class DockerImageController {

    @Inject
    private Instance<DockerClientExecutor> dockerClientExecutorInstance;

    public void removeImage(@Observes AfterDestroy event, CubeRegistry registry, CubeDockerConfiguration configuration) {
        if (configuration.isCleanBuildImage()) {
            Cube cube = validateAndGet(registry, event.getCubeId());
            CubeContainer config = (CubeContainer) cube.configuration();
            String imageRef;
            if (config.getImage() != null) {
                imageRef = config.getImage().toImageRef();
            } else {
                imageRef = event.getCubeId() + ":latest";  // building image from Dockerfile.
            }
            DockerClientExecutor executor = dockerClientExecutorInstance.get();
            executor.removeImage(imageRef, false);
        }
    }
}
