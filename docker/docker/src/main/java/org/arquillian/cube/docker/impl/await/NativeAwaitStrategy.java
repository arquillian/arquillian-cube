package org.arquillian.cube.docker.impl.await;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Cube;

public class NativeAwaitStrategy implements AwaitStrategy {

    public static final String TAG = "native";

    private DockerClientExecutor dockerClientExecutor;
    private String cubeId;

    public NativeAwaitStrategy(Cube<?> cube, DockerClientExecutor dockerClientExecutor ) {
        this.dockerClientExecutor = dockerClientExecutor;
        this.cubeId = cube.getId();
    }

    @Override
    public boolean await() {

        if (this.dockerClientExecutor.waitContainer(this.cubeId) == 0) {
            return true;
        } else {
            return false;
        }
    }

}
