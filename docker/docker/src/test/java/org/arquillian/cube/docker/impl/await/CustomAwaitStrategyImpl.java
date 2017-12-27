package org.arquillian.cube.docker.impl.await;

import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.await.AwaitStrategy;
import org.junit.experimental.categories.Category;

@Category({RequiresDocker.class, RequiresDockerMachine.class})
public class CustomAwaitStrategyImpl implements AwaitStrategy {

    Await params;
    DockerClientExecutor dockerClientExecutor;
    Cube<?> cube;

    public void setCube(Cube<?> cube) {
        this.cube = cube;
    }

    public void setDockerClientExecutor(DockerClientExecutor dockerClientExecutor) {
        this.dockerClientExecutor = dockerClientExecutor;
    }

    @Override
    public boolean await() {
        return this.params != null && this.dockerClientExecutor != null && this.cube != null;
    }
}
