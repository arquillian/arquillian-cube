package org.arquillian.cube.docker.junit5;

import java.io.File;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.BindMode;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.Container;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.ContainerBuilder;
import org.arquillian.cube.docker.junit.rule.ContainerDslRule;
import org.arquillian.cube.docker.junit.rule.NetworkDslRule;
import org.arquillian.cube.spi.CubeOutput;
import org.jboss.shrinkwrap.api.Archive;

public class ContainerDsl {

    private ContainerBuilder.ContainerOptionsBuilder containerBuilder;
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

    public ContainerDsl withVolume(String hostPath, String containerPath) {
        return withVolume(hostPath, containerPath, BindMode.READ_WRITE);
    }

    public ContainerDsl withVolume(String hostPath, String containerPath, BindMode bindMode) {
        containerBuilder.withVolume(hostPath, containerPath, bindMode);
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
