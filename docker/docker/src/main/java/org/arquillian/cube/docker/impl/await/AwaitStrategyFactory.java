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

    /**
     * Build an await strategy for the provided Cube
     *
     * @param dockerClientExecutor Docker client
     * @param cube                 The cube for which the strategy is required
     * @param options              The container options
     * @return The configure await strategy, ot the default polling strategy
     */
    public static AwaitStrategy create(final DockerClientExecutor dockerClientExecutor,
                                       final Cube<?> cube,
                                       final CubeContainer options) {

        if (options.getAwait() != null) {
            Await await = options.getAwait();

            if (await.getStrategy() != null) {

                await.setIp((dockerClientExecutor.isDockerInsideDockerResolution()
                    ? dockerClientExecutor.getDockerServerIp() : dockerClientExecutor.getDockerUri().getHost()));

                switch (await.getStrategy().toLowerCase()) {
                    case PollingAwaitStrategy.TAG:
                        return new PollingAwaitStrategy(cube, dockerClientExecutor, await);
                    case StaticAwaitStrategy.TAG:
                        return new StaticAwaitStrategy(cube, dockerClientExecutor, await);
                    case HttpAwaitStrategy.TAG:
                        return new HttpAwaitStrategy(cube, dockerClientExecutor, await);
                    case LogScanningAwaitStrategy.TAG:
                        return new LogScanningAwaitStrategy(cube, dockerClientExecutor, await);
                    case NativeAwaitStrategy.TAG:
                        return new NativeAwaitStrategy(cube, dockerClientExecutor);
                    case SleepingAwaitStrategy.TAG:
                        return new SleepingAwaitStrategy(cube, await);
                    case DockerHealthAwaitStrategy.TAG:
                        return new DockerHealthAwaitStrategy(cube, dockerClientExecutor, await);
                    default:
                        return new CustomAwaitStrategyInstantiator(cube, dockerClientExecutor, await);
                }
            } else {
                log.fine("No await strategy is set and Polling one is going to be used.");
            }
        } else {
            log.fine("No await strategy is set and Polling strategy is going to be used.");
        }

        return new PollingAwaitStrategy(cube, dockerClientExecutor, new Await());
    }
}
