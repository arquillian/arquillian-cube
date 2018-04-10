package org.arquillian.cube.docker.junit.rule;

import org.arquillian.cube.docker.impl.client.SystemPropertiesCubeSetter;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.BindMode;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.Container;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.ContainerBuilder;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.docker.junit.DockerClientInitializer;
import org.arquillian.cube.docker.junit.Injector;
import org.arquillian.cube.impl.model.LocalCubeRegistry;
import org.arquillian.cube.spi.CubeOutput;
import org.arquillian.cube.spi.event.lifecycle.AfterDestroy;
import org.arquillian.cube.spi.event.lifecycle.AfterStart;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ContainerDslRule implements TestRule {


    private DockerClientExecutor dockerClientExecutor;
    private SystemPropertiesCubeSetter systemPropertiesCubeSetter = new SystemPropertiesCubeSetter();

    private ContainerBuilder.ContainerOptionsBuilder containerBuilder;
    private Container container;

    public ContainerDslRule(File directory, String imageId) {
        this.containerBuilder = Container.withContainerName(imageId)
            .fromBuildDirectory(directory.getAbsolutePath());
        initializeDockerClient();

    }

    public ContainerDslRule(Archive<?> buildDirectory, String imageId) {
        this.containerBuilder = Container.withContainerName(imageId)
            .fromBuildDirectory(buildDirectory);
        initializeDockerClient();

    }

    public ContainerDslRule(String image) {
        this(image, convertImageToId(image));
    }

    public ContainerDslRule(String image, String id) {
        this.containerBuilder = Container.withContainerName(id)
            .fromImage(image);
        initializeDockerClient();
    }

    private static String convertImageToId(String imageId) {
        return imageId
                        .replace('/', '_')
                        .replace(':', '_')
                        .replace('.', '_');
    }

    private void initializeDockerClient() {
        this.dockerClientExecutor = DockerClientInitializer.initialize();
        // Since we are out of arquillian runner we need to call events by ourselves.
        this.systemPropertiesCubeSetter.createDockerHostProperty(new BeforeSuite(), this.dockerClientExecutor);
    }

    public ContainerDslRule withExposedPorts(Integer... ports) {
        containerBuilder.withExposedPorts(ports);
        return this;
    }

    public ContainerDslRule withExposedPorts(String... ports) {
        containerBuilder.withExposedPorts(ports);
        return this;
    }

    public ContainerDslRule withPortBinding(Integer... ports) {
        containerBuilder.withPortBinding(ports);
        return this;
    }

    public ContainerDslRule withPortBinding(String... ports) {
        containerBuilder.withPortBinding(ports);
        return this;
    }

    public ContainerDslRule withEnvironment(String key, Object value, Object...keyValues) {
        containerBuilder.withEnvironment(key, value, keyValues);
        return this;
    }

    public ContainerDslRule withCommand(String command) {
        containerBuilder.withCommand(command);
        return this;
    }

    public ContainerDslRule withCommand(String... command) {
        containerBuilder.withCommand(command);
        return this;
    }

    public ContainerDslRule withVolume(String hostPath, String containerPath) {
        return withVolume(hostPath, containerPath, BindMode.READ_WRITE);
    }

    public ContainerDslRule withVolume(String hostPath, String containerPath, BindMode bindMode) {
        containerBuilder.withVolume(hostPath, containerPath, bindMode);
        return this;
    }

    public ContainerDslRule withNetworkMode(String networkMode) {
        containerBuilder.withNetworkMode(networkMode);
        return this;
    }

    public ContainerDslRule withNetworkMode(NetworkDslRule networkMode) {
        return this.withNetworkMode(networkMode.getNetworkName());
    }

    public ContainerDslRule withNetworks(String... networks) {
        containerBuilder.withNetworks(networks);
        return this;
    }

    public ContainerDslRule withPriviledgedMode(boolean mode) {
        containerBuilder.withPriviledgedMode(mode);
        return this;
    }

    public ContainerDslRule withLink(String link) {
        containerBuilder.withLink(link);
        return this;
    }

    public ContainerDslRule withLink(String service, String alias) {
        return withLink(service + ":" + alias);
    }

    public ContainerDslRule withAwaitStrategy(Await awaitStrategy) {
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

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {

                List<Throwable> errors = new ArrayList<>();

                // Inject Arquillian resources
                container = containerBuilder.build();
                final LocalCubeRegistry localCubeRegistry = new LocalCubeRegistry();
                final DockerCube dockerCube = Injector.prepareContainer(container, dockerClientExecutor, localCubeRegistry);

                try {
                    dockerCube.create();
                    dockerCube.start();

                    systemPropertiesCubeSetter.createCubeSystemProperties(new AfterStart(dockerCube.getId()), localCubeRegistry);

                    // Run tests
                    base.evaluate();

                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    // stop container
                    dockerCube.stop();
                    dockerCube.destroy();

                    systemPropertiesCubeSetter.removeCubeSystemProperties(new AfterDestroy(dockerCube.getId()));
                    systemPropertiesCubeSetter.removeDockerHostProperty(new AfterSuite());
                }

                MultipleFailureException.assertEmpty(errors);

            }
        };
    }
}
