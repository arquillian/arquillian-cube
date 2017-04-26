package org.arquillian.cube.docker.junit.rule;

import org.arquillian.cube.HostIpContext;
import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.client.CubeDockerConfigurationResolver;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.client.config.BuildImage;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.BindMode;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.Container;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.ContainerBuilder;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.docker.impl.util.Boot2Docker;
import org.arquillian.cube.docker.impl.util.CommandLineExecutor;
import org.arquillian.cube.docker.impl.util.DockerMachine;
import org.arquillian.cube.docker.impl.util.OperatingSystemResolver;
import org.arquillian.cube.docker.impl.util.Top;
import org.arquillian.cube.impl.model.LocalCubeRegistry;
import org.arquillian.cube.spi.CubeOutput;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.CubeLifecyleEvent;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ContainerDslRule implements TestRule {


    private DockerClientExecutor dockerClientExecutor;

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
                final Optional<Field> hostIpContextField = Reflections.findFieldByGenericType(Container.class, Instance.class, HostIpContext.class);

                if (hostIpContextField.isPresent()) {
                    Reflections.injectObject(container, hostIpContextField.get(), (Instance) () -> new HostIpContext(dockerClientExecutor.getDockerServerIp()));
                }

                final Optional<Field> dockerClientExecutorField = Reflections.findFieldByGenericType(Container.class, Instance.class, DockerClientExecutor.class);

                if (dockerClientExecutorField.isPresent()) {
                    Reflections.injectObject(container, dockerClientExecutorField.get(), (Instance) () -> dockerClientExecutor);
                }

                DockerCube dockerCube = new DockerCube(container.getContainerName(), container.getCubeContainer(), dockerClientExecutor);
                LocalCubeRegistry localCubeRegistry = new LocalCubeRegistry();
                localCubeRegistry.addCube(dockerCube);

                final Optional<Field> cubeRegistryField = Reflections.findFieldByGenericType(Container.class, Instance.class, CubeRegistry.class);

                if (cubeRegistryField.isPresent()) {
                    Reflections.injectObject(container, cubeRegistryField.get(), (Instance) () -> localCubeRegistry);
                }

                final Optional<Field> eventField = Reflections.findFieldByGenericType(DockerCube.class, Event.class, CubeLifecyleEvent.class);

                if (eventField.isPresent()) {
                    Reflections.injectObject(dockerCube, eventField.get(), (Event) o -> {
                    });
                }


                try {
                    dockerCube.create();
                    dockerCube.start();

                    // Run tests
                    base.evaluate();

                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    // stop container
                    dockerCube.stop();
                    dockerCube.destroy();
                }

                MultipleFailureException.assertEmpty(errors);

            }
        };
    }
}
