package org.arquillian.cube.docker.impl.client.containerobject;

import java.util.Optional;

import org.arquillian.cube.ContainerObjectConfiguration;
import org.arquillian.cube.docker.impl.await.PollingAwaitStrategy;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.Link;
import org.arquillian.cube.docker.impl.client.config.PortBinding;

/**
 * This implementation of {@link ContainerObjectConfiguration} is a bridge to {@link CubeContainer}
 * Clients of this class are encouraged to access the internal {@link CubeContainer} for additional options
 *
 * @author <a href="mailto:rivasdiaz@gmail.com">Ramon Rivas</a>
 */
public class CubeContainerObjectConfiguration implements ContainerObjectConfiguration {

    private final CubeContainer configuration;

    public CubeContainerObjectConfiguration(CubeContainer configuration) {
        this.configuration = configuration;
    }

    public CubeContainer getCubeContainerConfiguration() {
        return configuration;
    }

    public static CubeContainerObjectConfiguration empty() {
        return new CubeContainerObjectConfiguration(null);
    }

    @Override
    public String getContainerName() {
        return configuration.getContainerName();
    }

    @Override
    public String[] getPortBindings() {
        return Optional.ofNullable(configuration.getPortBindings())
                .map(links -> links.stream().map(PortBinding::toString).toArray(String[]::new))
                .orElse(null);
    }

    @Override
    public int[] getAwaitPorts() {
        return Optional.ofNullable(configuration.getAwait())
                .filter(await -> await.getStrategy().equals(PollingAwaitStrategy.TAG))
                .map(Await::getPorts)
                .map(ports -> ports.stream().mapToInt(Integer::intValue).toArray())
                .orElse(null);
    }

    @Override
    public String[] getEnvironmentVariables() {
        return Optional.ofNullable(configuration.getEnv())
                .map(env -> env.stream().toArray(String[]::new))
                .orElse(null);
    }

    @Override
    public String[] getVolumes() {
        return Optional.ofNullable(configuration.getVolumes())
                .map(volumes -> volumes.stream().toArray(String[]::new))
                .orElse(null);
    }

    @Override
    public String[] getLinks() {
        return Optional.ofNullable(configuration.getLinks())
                .map(links -> links.stream().map(Link::toString).toArray(String[]::new))
                .orElse(null);
    }
}
