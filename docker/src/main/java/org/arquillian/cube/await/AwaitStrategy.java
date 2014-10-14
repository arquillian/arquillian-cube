package org.arquillian.cube.await;

import com.github.dockerjava.api.command.CreateContainerResponse;

public interface AwaitStrategy {

    boolean await();
    
}
