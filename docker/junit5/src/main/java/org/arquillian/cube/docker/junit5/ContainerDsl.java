package org.arquillian.cube.docker.junit5;

import java.io.File;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.BindMode;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.Container;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.ContainerBuilder;
import org.arquillian.cube.spi.CubeOutput;
import org.jboss.shrinkwrap.api.Archive;

public class ContainerDsl {

    private final ContainerBuilder.ContainerOptionsBuilder containerBuilder;
    private Container container;

    public ContainerDsl(File directory, String imageId) {
        this.containerBuilder = Container.withContainerName(imageId)
            .fromBuildDirectory(directory.getAbsolutePath());

    }

    public ContainerDsl(Archive<?> buildDirectory, String imageId) {
        this.containerBuilder = Container.withContainerName(imageId)
            .fromBuildDirectory(buildDirectory);

    }

    public ContainerDsl(String image) {
        this(image, convertImageToId(image));
    }

    public ContainerDsl(String image, String id) {
        this.containerBuilder = Container.withContainerName(id)
            .fromImage(image);
    }

    private static String convertImageToId(String imageId) {
        return imageId
            .replace('/', '_')
            .replace(':', '_')
            .replace('.', '_');
    }

    public ContainerDsl withExposedPorts(Integer... ports) {
        containerBuilder.withExposedPorts(ports);
        return this;
    }

    public ContainerDsl withExposedPorts(String... ports) {
        containerBuilder.withExposedPorts(ports);
        return this;
    }

    public ContainerDsl withPortBinding(Integer... ports) {
        containerBuilder.withPortBinding(ports);
        return this;
    }

    public ContainerDsl withPortBinding(String... ports) {
        containerBuilder.withPortBinding(ports);
        return this;
    }

    public ContainerDsl withEnvironment(String key, Object value, Object...keyValues) {
        containerBuilder.withEnvironment(key, value, keyValues);
        return this;
    }

    public ContainerDsl withCommand(String command) {
        containerBuilder.withCommand(command);
        return this;
    }

    public ContainerDsl withCommand(String... command) {
        containerBuilder.withCommand(command);
        return this;
    }

    /**
     * Configure volume mount on the Docker container defined by the DSL based definition,
     * setting {@link BindMode#READ_WRITE} as the default volume.
     * mount option
     * @param hostPath Path of the host directory that should be mounted on the container
     * @param containerPath Path of the container directory where the volume should be mounted
     * @return The current {@link ContainerDsl} instance holding the configuration
     *
     * @deprecated The current Docker documentation doesn't mention {@code rw} as a valid option.
     */
    public ContainerDsl withVolume(String hostPath, String containerPath) {
        return withVolume(hostPath, containerPath, BindMode.READ_WRITE);
    }

    /**
     * Configure volume mount on the Docker container defined by the DSL based definition.
     * @param hostPath Path of the host directory that should be mounted on the container
     * @param containerPath Path of the container directory where the volume should be mounted
     * @param bindMode {@link BindMode} enumeration value representing a valid option for the volume mount
     *  configuration, based on {@code docker-java} {@link com.github.dockerjava.api.model.AccessMode}
     * @return The current {@link ContainerDsl} instance holding the configuration
     */
    public ContainerDsl withVolume(String hostPath, String containerPath, BindMode bindMode) {
        containerBuilder.withVolume(hostPath, containerPath, bindMode);
        return this;
    }

    /**
     * Configure volume mount on the Docker container defined by the DSL based definition.
     * @param hostPath Path of the host directory that should be mounted on the container
     * @param containerPath Path of the container directory where the volume should be mounted
     * @param bindModeOption String representing a valid option for the volume mount configuration, see the
     *  <a href="https://docs.docker.com/engine/storage/bind-mounts/#options-for---volume">Docker documentation</a>
     * @return The current {@link ContainerDsl} instance holding the configuration
     */
    public ContainerDsl withVolume(String hostPath, String containerPath, String bindModeOption) {
        containerBuilder.withVolume(hostPath, containerPath, bindModeOption);
        return this;
    }

    public ContainerDsl withNetworkMode(String networkMode) {
        containerBuilder.withNetworkMode(networkMode);
        return this;
    }

    public ContainerDsl withNetworkMode(NetworkDsl networkMode) {
        return this.withNetworkMode(networkMode.getNetworkName());
    }

    public ContainerDsl withNetworks(String... networks) {
        containerBuilder.withNetworks(networks);
        return this;
    }

    public ContainerDsl withPriviledgedMode(boolean mode) {
        containerBuilder.withPriviledgedMode(mode);
        return this;
    }

    public ContainerDsl withLink(String link) {
        containerBuilder.withLink(link);
        return this;
    }

    public ContainerDsl withLink(String service, String alias) {
        return withLink(service + ":" + alias);
    }

    public ContainerDsl withAwaitStrategy(Await awaitStrategy) {
        containerBuilder.withAwaitStrategy(awaitStrategy);
        return this;
    }

    public String getIpAddress() {
        return this.container.getIpAddress();
    }

    public int getBindPort(int exposedPort) {
        return this.container.getBindPort(exposedPort);
    }

    public String getLog() {
        return this.container.getLog();
    }

    public CubeOutput exec(String... commands) {
        return this.container.exec(commands);
    }

    Container buildContainer() {
        this.container = containerBuilder.build();
        return this.container;
    }

}
