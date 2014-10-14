package org.arquillian.cube.await;

import java.util.Map;

import org.arquillian.cube.docker.DockerClientExecutor;

import com.github.dockerjava.api.command.CreateContainerResponse;

public class AwaitStrategyFactory {

    private static final String AWAIT = "await";
    private static final String STRATEGY = "strategy";
    
    private AwaitStrategyFactory() {
        super();
    }
    
    public static final AwaitStrategy create(DockerClientExecutor dockerClientExecutor, CreateContainerResponse createContainerResponse, Map<String, Object> options) {
        
        if(options.containsKey(AWAIT)) {
            
            Map<String, Object> awaitOptions = (Map<String, Object>) options.get(AWAIT);
            
            if(awaitOptions.containsKey(STRATEGY)) {
                
                String strategy = ((String) awaitOptions.get(STRATEGY)).toLowerCase();
                
                switch(strategy) {
                    case PollingAwaitStrategy.TAG: return new PollingAwaitStrategy(dockerClientExecutor, createContainerResponse);
                    case NativeAwaitStrategy.TAG: return new NativeAwaitStrategy(dockerClientExecutor, createContainerResponse);
                    case StaticAwaitStrategy.TAG: return new StaticAwaitStrategy(awaitOptions);
                    default: return new NativeAwaitStrategy(dockerClientExecutor, createContainerResponse);
                }
                
            } else {
                return new NativeAwaitStrategy(dockerClientExecutor, createContainerResponse);
            }
            
        } else {
            return new NativeAwaitStrategy(dockerClientExecutor, createContainerResponse);
        }
        
    }
    
    
}
