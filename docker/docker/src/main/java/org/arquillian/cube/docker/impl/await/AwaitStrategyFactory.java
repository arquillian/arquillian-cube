package org.arquillian.cube.docker.impl.await;

import java.util.logging.Logger;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.await.AwaitStrategy;

public class AwaitStrategyFactory {

    private static final Logger log = Logger.getLogger(AwaitStrategyFactory.class.getName());

    private AwaitStrategyFactory() {
        super();
    }

    public static final AwaitStrategy create(DockerClientExecutor dockerClientExecutor, Cube<?> cube,
        CubeContainer options) {

        if (options.getAwait() != null) {
            Await await = options.getAwait();

            if (await.getStrategy() != null) {

                String strategy = await.getStrategy().toLowerCase();
                switch (strategy) {
                    case PollingAwaitStrategy.TAG:
                        return new PollingAwaitStrategy(cube, dockerClientExecutor, await);
                    case LogScanningAwaitStrategy.TAG:
                        return new LogScanningAwaitStrategy(cube, dockerClientExecutor, await);
                    case NativeAwaitStrategy.TAG:
                        return new NativeAwaitStrategy(cube, dockerClientExecutor);
                    case StaticAwaitStrategy.TAG:
                        return new StaticAwaitStrategy(cube, await);
                    case SleepingAwaitStrategy.TAG:
                        return new SleepingAwaitStrategy(cube, await);
                    case HttpAwaitStrategy.TAG:
                        return new HttpAwaitStrategy(cube, dockerClientExecutor, await);
                    case DockerHealthAwaitStrategy.TAG:
                        return new DockerHealthAwaitStrategy(cube, dockerClientExecutor, await);
                    default:
                        return new CustomAwaitStrategyInstantiator(cube, dockerClientExecutor, await);
                }
            } else {
                log.fine("No await strategy is set and Polling one is going to be used.");
                return new PollingAwaitStrategy(cube, dockerClientExecutor, new Await());
            }
        } else {
            log.fine("No await strategy is set and Polling strategy is going to be used.");
            return new PollingAwaitStrategy(cube, dockerClientExecutor, new Await());
        }
    }
}
