package org.arquillian.cube.impl.await;

import java.util.Map;
import java.util.logging.Logger;

import org.arquillian.cube.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Cube;

public class AwaitStrategyFactory {

    private static final Logger log = Logger.getLogger(AwaitStrategyFactory.class.getName());

    private static final String AWAIT = "await";
    private static final String STRATEGY = "strategy";

    private AwaitStrategyFactory() {
        super();
    }

    public static final AwaitStrategy create(DockerClientExecutor dockerClientExecutor, Cube cube, Map<String, Object> options) {

        if(options.containsKey(AWAIT)) {

            @SuppressWarnings("unchecked")
            Map<String, Object> awaitOptions = (Map<String, Object>) options.get(AWAIT);

            if (awaitOptions.containsKey(STRATEGY)) {

                String strategy = ((String) awaitOptions.get(STRATEGY)).toLowerCase();
                switch(strategy) {
                    case PollingAwaitStrategy.TAG: return new PollingAwaitStrategy(cube);
                    case NativeAwaitStrategy.TAG: return new NativeAwaitStrategy(cube, dockerClientExecutor);
                    case StaticAwaitStrategy.TAG: return new StaticAwaitStrategy(cube, awaitOptions);
                    default: return new NativeAwaitStrategy(cube, dockerClientExecutor);
                }

            } else {
                log.warning("No await strategy is set and Native one is going to be used.");
                return new NativeAwaitStrategy(cube, dockerClientExecutor);
            }

        } else {
            log.warning("No await strategy is set and Native one is going to be used.");
            return new NativeAwaitStrategy(cube, dockerClientExecutor);
        }
    }
}
