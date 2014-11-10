package org.arquillian.cube.await;

import org.arquillian.cube.docker.DockerClientExecutor;

public class NativeAwaitStrategy implements AwaitStrategy {

    public static final String TAG = "native";
    
    private DockerClientExecutor dockerClientExecutor;
    private String containerId;
    
    public NativeAwaitStrategy(DockerClientExecutor dockerClientExecutor, String containerId) {
        this.dockerClientExecutor = dockerClientExecutor;
        this.containerId = containerId;
    }
    
    @Override
    public boolean await() {

        if (this.dockerClientExecutor.waitContainer(this.containerId) == 0) {
            return true;
        } else {
            return false;
        }
    }

}
