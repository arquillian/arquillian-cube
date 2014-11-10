package org.arquillian.cube.await;

import java.util.Map;
import java.util.logging.Logger;

import org.arquillian.cube.docker.DockerClientExecutor;

public class AwaitStrategyFactory {

    private static final Logger log = Logger.getLogger(AwaitStrategyFactory.class.getName());
    
    private static final String AWAIT = "await";
    private static final String STRATEGY = "strategy";

    private AwaitStrategyFactory() {
        super();
    }

    public static final AwaitStrategy create(DockerClientExecutor dockerClientExecutor, String containerId,
            Map<String, Object> options) {

        if (options.containsKey(AWAIT)) {

            Map<String, Object> awaitOptions = (Map<String, Object>) options.get(AWAIT);

            if (awaitOptions.containsKey(STRATEGY)) {

                String strategy = ((String) awaitOptions.get(STRATEGY)).toLowerCase();

                switch (strategy) {
                case PollingAwaitStrategy.TAG:
                    return new PollingAwaitStrategy(dockerClientExecutor, containerId);
                case NativeAwaitStrategy.TAG:
                    return new NativeAwaitStrategy(dockerClientExecutor, containerId);
                case StaticAwaitStrategy.TAG:
                    return new StaticAwaitStrategy(awaitOptions);
                default:
                    return new NativeAwaitStrategy(dockerClientExecutor, containerId);
                }

            } else {
                log.warning("No await strategy is set and Native one is going to be used.");
                return new NativeAwaitStrategy(dockerClientExecutor, containerId);
            }

        } else {
            log.warning("No await strategy is set and Native one is going to be used.");
            return new NativeAwaitStrategy(dockerClientExecutor, containerId);
        }

    }

}
