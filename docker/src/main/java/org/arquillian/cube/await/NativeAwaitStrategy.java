package org.arquillian.cube.await;

import org.arquillian.cube.docker.DockerClientExecutor;

import com.github.dockerjava.api.command.CreateContainerResponse;

public class NativeAwaitStrategy implements AwaitStrategy {

    public static final String TAG = "native";
    
    private DockerClientExecutor dockerClientExecutor;
    private CreateContainerResponse createContainerResponse;
    
    public NativeAwaitStrategy(DockerClientExecutor dockerClientExecutor, CreateContainerResponse createContainerResponse) {
        this.dockerClientExecutor = dockerClientExecutor;
        this.createContainerResponse = createContainerResponse;
    }
    
    @Override
    public boolean await() {

        if (this.dockerClientExecutor.waitContainer(this.createContainerResponse) == 0) {
            return true;
        } else {
            return false;
        }
    }

}
