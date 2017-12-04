package org.arquillian.cube.docker.impl.client.containerobject.dsl;

import org.arquillian.cube.containerobject.ConnectionMode;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.client.config.BuildImage;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.ExposedPort;
import org.arquillian.cube.docker.impl.client.config.Image;
import org.arquillian.cube.docker.impl.client.config.Link;
import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.arquillian.cube.docker.impl.client.config.StarOperator;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Builder object to create containers object.
 */
public class ContainerBuilder {

    private static final String TEMPORARY_FOLDER_PREFIX = "arquilliancube_";
    private static final String TEMPORARY_FOLDER_SUFFIX = ".build";

    private static final Logger logger = Logger.getLogger(ContainerBuilder.class.getName());

    private String id;
    private CubeContainer cubeContainer = new CubeContainer();

    private ContainerBuilder(String id) {
        this.id = id;
    }

    public static ContainerBuilder newContainer(String id) {
        return new ContainerBuilder(id);
    }

    public static ContainerBuilder newContainerFromImage(String image) {
        ContainerBuilder containerBuilder = new ContainerBuilder(convertImageToId(image));
        containerBuilder.fromImage(image);

        return containerBuilder;
    }

    private static String convertImageToId(String imageId) {
        return imageId
            .replace('/', '_')
            .replace(':', '_')
            .replace('.', '_');
    }

    public ContainerOptionsBuilder fromImage(String image) {
        cubeContainer.setImage(Image.valueOf(image));
        return new ContainerOptionsBuilder();
    }

    public ContainerOptionsBuilder fromBuildDirectory(String directory) {
        BuildImage buildImage = new BuildImage(directory, null, true, true);
        cubeContainer.setBuildImage(buildImage);

        return new ContainerOptionsBuilder();
    }

    public ContainerOptionsBuilder fromBuildDirectory(Archive<?> buildDirectory) {
        File output = createTemporalDirectoryForCopyingDockerfile(id);
        logger.finer(String.format("Created %s directory for storing contents of %s cube.", output, id));

        buildDirectory.as(ExplodedExporter.class).exportExplodedInto(output);
        return fromBuildDirectory(output.getAbsolutePath());
    }

    private File createTemporalDirectoryForCopyingDockerfile(String containerName) {
        File dir;
        try {
            dir = File.createTempFile(TEMPORARY_FOLDER_PREFIX+containerName, TEMPORARY_FOLDER_SUFFIX);
            dir.delete();
            if (!dir.mkdirs()) {
                throw new IllegalArgumentException("Temp Dir for storing Dockerfile contents could not be created.");
            }
            dir.deleteOnExit();
            return dir;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public class ContainerOptionsBuilder {
        private ContainerBuilder containerBuilder;
        private ConnectionMode connectionMode = ConnectionMode.START_AND_STOP_AROUND_CLASS;

        public ContainerOptionsBuilder withConnectionMode(ConnectionMode connectionMode) {
            this.connectionMode = connectionMode;
            return this;
        }

        public ContainerOptionsBuilder withExposedPorts(Integer... ports) {
            List<ExposedPort> exposedPortList = Arrays.stream(ports)
                    .map(ExposedPort::valueOf)
                    .collect(Collectors.toList());
            setExposedPorts(exposedPortList);

            return this;
        }

        public ContainerOptionsBuilder withExposedPorts(String... ports) {
            List<ExposedPort> exposedPortList = Arrays.stream(ports)
                    .map(ExposedPort::valueOf)
                    .collect(Collectors.toList());
            setExposedPorts(exposedPortList);

            return this;
        }

        public ContainerOptionsBuilder withPortBinding(Integer... ports) {
            List<PortBinding> portBindingList = Arrays.stream(ports)
                    .map(port -> Integer.toString(port))
                    .map(PortBinding::valueOf)
                    .collect(Collectors.toList());
            setPortBinding(portBindingList);

            return this;
        }

        public ContainerOptionsBuilder withPortBinding(String... ports) {
            List<PortBinding> portBindingList = Arrays.stream(ports)
                    .map(PortBinding::valueOf)
                    .collect(Collectors.toList());
            setPortBinding(portBindingList);

            return this;
        }

        public ContainerOptionsBuilder withEnvironment(String key, Object value, Object...keyValues) {
            if (keyValues.length % 2 != 0) {
                throw new IllegalArgumentException("Key Values should be a pair of key, value");
            }

            List<String> environment = new ArrayList<>();
            environment.add(toEnv(key, value));

            for (int i = 0; i < keyValues.length; i += 2) {
                environment.add(toEnv(keyValues[i], keyValues[i + 1]));
            }

            setEnvironment(environment);

            return this;
        }

        public ContainerOptionsBuilder withCommand(String command) {
            cubeContainer.setCmd(Arrays.asList(command.split(" ")));

            return this;
        }

        public ContainerOptionsBuilder withCommand(String... command) {
            cubeContainer.setCmd(Arrays.asList(command));

            return this;
        }

        public ContainerOptionsBuilder withVolume(String hostPath, String containerPath) {
            return withVolume(hostPath, containerPath, BindMode.READ_WRITE);
        }

        public ContainerOptionsBuilder withVolume(String hostPath, String containerPath, BindMode bindMode) {
            setVolume(hostPath + ":" + containerPath + ":" + bindMode.accessMode.name());
            return this;
        }

        public ContainerOptionsBuilder withNetworkMode(String networkMode) {
            cubeContainer.setNetworkMode(networkMode);
            return this;
        }

        public ContainerOptionsBuilder withNetworkMode(Network networkMode) {
            return this.withNetworkMode(networkMode.getId());
        }

        public ContainerOptionsBuilder withNetworks(String... networks) {
            cubeContainer.setNetworks(Arrays.asList(networks));
            return this;
        }

        public ContainerOptionsBuilder withNetworks(Network... networks) {
            return withNetworks(Arrays.stream(networks)
                    .map(Network::getId)
                    .toArray( String[]::new)
            );

        }

        public ContainerOptionsBuilder withPriviledgedMode(boolean mode) {
            cubeContainer.setPrivileged(mode);
            return this;
        }

        public ContainerOptionsBuilder withLink(String link) {
            setLink(link);
            return this;
        }

        public ContainerOptionsBuilder withLink(String service, String alias) {
            return withLink(service + ":" + alias);
        }

        public ContainerOptionsBuilder withLink(Container container) {
            return withLink(container.getContainerName(), container.getContainerName());
        }

        public ContainerOptionsBuilder withAwaitStrategy(Await awaitStrategy) {
            cubeContainer.setAwait(awaitStrategy);
            return this;
        }

        public Container build() {

            if (id.endsWith("*")) {

                final UUID uuid = UUID.randomUUID();

                StarOperator.adaptPortBindingToParallelRun(cubeContainer);
                StarOperator.adaptLinksToParallelRun(uuid, cubeContainer);
                StarOperator.adaptDependenciesToParallelRun(uuid, cubeContainer);

                String templateName = id.substring(0, id.lastIndexOf('*'));
                id = StarOperator.generateNewName(templateName, uuid);
            }

            return new Container(id, cubeContainer, connectionMode);
        }

        private String toEnv(Object key, Object value) {
            return key.toString() + "=" + value.toString();
        }

        private void setExposedPorts(List<ExposedPort> exposedPorts) {
            if (cubeContainer.getExposedPorts() == null) {
                cubeContainer.setExposedPorts(exposedPorts);
            } else {
                cubeContainer.getExposedPorts().addAll(exposedPorts);
            }
        }

        private void setPortBinding(List<PortBinding> portBindings) {
            if (cubeContainer.getPortBindings() == null) {
                cubeContainer.setPortBindings(portBindings);
            } else {
                cubeContainer.getPortBindings().addAll(portBindings);
            }
        }

        private void setEnvironment(List<String> environments) {
            if (cubeContainer.getEnv() == null) {
                cubeContainer.setEnv(environments);
            } else {
                cubeContainer.getEnv().addAll(environments);
            }
        }

        private void setVolume(String volume) {
            if (cubeContainer.getBinds() == null) {
                List<String> binds = new ArrayList<>();
                binds.add(volume);

                cubeContainer.setBinds(binds);
            } else {
                cubeContainer.getBinds().add(volume);
            }
        }

        private void setLink(String link) {
            if (cubeContainer.getLinks() == null) {
                List<Link> links = new ArrayList<>();
                links.add(Link.valueOf(link));

                cubeContainer.setLinks(links);
            } else {
                cubeContainer.getLinks().add(Link.valueOf(link));
            }
        }

    }

}
