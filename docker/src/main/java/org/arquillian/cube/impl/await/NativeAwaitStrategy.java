package org.arquillian.cube.impl.await;

import org.arquillian.cube.impl.docker.DockerClientExecutor;

public class NativeAwaitStrategy implements AwaitStrategy {

    public static final String TAG = "native";

    private DockerClientExecutor dockerClientExecutor;
    private String cubeId;

    public NativeAwaitStrategy(DockerClientExecutor dockerClientExecutor, String cubeId) {
        this.dockerClientExecutor = dockerClientExecutor;
        this.cubeId = cubeId;
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
