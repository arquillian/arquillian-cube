package org.arquillian.cube.docker.impl.await;

import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.util.BindingUtil;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.await.AwaitStrategy;

import java.util.logging.Logger;

public class AwaitStrategyFactory {

    private static final Logger log = Logger.getLogger(AwaitStrategyFactory.class.getName());

    private AwaitStrategyFactory() {
        super();
    }

    /**
     * Build an await strategy, assuming that the environment is not docker-in-docker
     *
     * @param dockerClientExecutor Docker client
     * @param cube                 The cube for which the strategy is required
     * @param options              The container options
     * @return The configure await strategy, or the default polling strategy
     */
    public static AwaitStrategy create(final DockerClientExecutor dockerClientExecutor,
                                       final Cube<?> cube,
                                       final CubeContainer options) {
        return create(dockerClientExecutor, cube, options, false);
    }

    /**
     * Build an await strategy with the option of defining that the environment is not docker-in-docker
     *
     * @param dockerClientExecutor Docker client
     * @param cube                 The cube for which the strategy is required
     * @param options              The container options
     * @param dind                 Is the docker daemon docker-in-docker (ie. dockerHost is NOT local)
     * @return The configure await strategy, ot the default polling strategy
     */
    public static AwaitStrategy create(final DockerClientExecutor dockerClientExecutor,
                                       final Cube<?> cube,
                                       final CubeContainer options,
                                       final boolean dind) {

        if (options.getAwait() != null) {
            Await await = options.getAwait();

            if (await.getStrategy() != null) {

                if (dind) {
                    final String containerIp = BindingUtil.getContainerIp(dockerClientExecutor, cube.getId());
                    await.setIp(containerIp);
                }

                String strategy = await.getStrategy().toLowerCase();
                switch (strategy) {
                    case PollingAwaitStrategy.TAG:
                        return new PollingAwaitStrategy(cube, dockerClientExecutor, await, dind);
                    case LogScanningAwaitStrategy.TAG:
                        return new LogScanningAwaitStrategy(cube, dockerClientExecutor, await);
                    case NativeAwaitStrategy.TAG:
                        return new NativeAwaitStrategy(cube, dockerClientExecutor);
                    case StaticAwaitStrategy.TAG:
                        return new StaticAwaitStrategy(cube, dockerClientExecutor, await, dind);
                    case SleepingAwaitStrategy.TAG:
                        return new SleepingAwaitStrategy(cube, await);
                    case HttpAwaitStrategy.TAG:
                        return new HttpAwaitStrategy(cube, dockerClientExecutor, await, dind);
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

        return new PollingAwaitStrategy(cube, dockerClientExecutor, new Await(), dind);
    }
}
