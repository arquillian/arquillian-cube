package org.arquillian.cube.docker.impl.util;

import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.client.config.PortBinding;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Utility class that helps on autoresolution of binding ports in case of running Arquillian as client mode and not using
 * enrichments for getting the host/bind port of exposed one.
 * <p>
 * Usually this class is used by extensions that needs autoresolution.
 */
public class SinglePortBindResolver {

    private static final int NO_PORT = -1;

    private SinglePortBindResolver() {
        super();
    }

    /**
     * Method that tries to resolve a bind port for a given exposed port.
     *
     * @param cubeDockerConfiguration where all docker configuration is exposed
     * @param exposedPort             used to find the binding port
     * @param excludedContainers      where binding port search is ignored
     * @return binding port
     */
    public static int resolveBindPort(CubeDockerConfiguration cubeDockerConfiguration, int exposedPort, String... excludedContainers) {
        final
        PortBindInfo portBinding = resolvePortBindPort(cubeDockerConfiguration, exposedPort, excludedContainers);

        if (portBinding == null) {
            return exposedPort;
        }

        return portBinding.getBindPort();
    }

    /**
     * Method that tries to resolve a bind port for a given exposed port.
     *
     * @param cubeDockerConfiguration where all docker configuration is exposed
     * @param exposedPort             used to find the binding port
     * @param excludedContainers      where binding port search is ignored
     * @return binding port or null if couldn't be found
     */
    public static PortBindInfo resolvePortBindPort(CubeDockerConfiguration cubeDockerConfiguration, int exposedPort, String... excludedContainers) {

        final DockerCompositions dockerContainersContent = cubeDockerConfiguration.getDockerContainersContent();
        final Set<Map.Entry<String, CubeContainer>> containers = dockerContainersContent.getContainers().entrySet();

        // user specified an exposed port
        PortBindInfo portBindInfo = null;
        for (Map.Entry<String, CubeContainer> cubeContainerEntry : containers) {

            // need to skip vnc and selenium container
            if (shouldBeIgnored(cubeContainerEntry.getKey(), excludedContainers)) {
                continue;
            }

            final CubeContainer cubeContainer = cubeContainerEntry.getValue();
            final Collection<PortBinding> portBindings = cubeContainer.getPortBindings();
            if (portBindings != null) {
                for (PortBinding portBinding : portBindings) {
                    if (portBinding.getExposedPort().getExposed() == exposedPort) {
                        if (noPreviousBindPortFound(portBindInfo)) {
                            int bindPort = portBinding.getBound();
                            portBindInfo = new PortBindInfo(
                                    portBinding.getExposedPort().getExposed(),
                                    bindPort, cubeContainerEntry.getKey());
                        } else {
                            throw new IllegalArgumentException(String.format("More than one docker container with port binding having exposed port %s.", exposedPort));
                        }
                    }
                }
            }

            if (noPreviousBindPortFound(portBindInfo)) {
                return null;
            }
        }
        return portBindInfo;
    }

    /**
     * Method that tries to resolve a bind port by searching if there is only one binding port across all running containers
     *
     * @param cubeDockerConfiguration where all docker configuration is exposed
     * @param excludedContainers      where binding port search is ignored
     * @return binding port
     */
    public static int resolveBindPort(CubeDockerConfiguration cubeDockerConfiguration, String... excludedContainers) {
        final PortBindInfo portBinding = resolvePortBindPort(cubeDockerConfiguration, excludedContainers);

        if (portBinding == null) {
            throw new IllegalArgumentException("There isn't any bind port.");
        }
        return portBinding.getBindPort();
    }

    /**
     * Method that tries to resolve a bind port by searching if there is only one binding port across all running containers
     *
     * @param cubeDockerConfiguration where all docker configuration is exposed
     * @param excludedContainers      where binding port search is ignored
     * @return binding port
     */
    public static PortBindInfo resolvePortBindPort(CubeDockerConfiguration cubeDockerConfiguration, String... excludedContainers) {

        final DockerCompositions dockerContainersContent = cubeDockerConfiguration.getDockerContainersContent();
        final Set<Map.Entry<String, CubeContainer>> containers = dockerContainersContent.getContainers().entrySet();

        //if no port, we check if there is only one cube with one bind port and if not, return default one

        PortBindInfo portBindInfo = null;
        for (Map.Entry<String, CubeContainer> cubeContainerEntry : containers) {

            // need to skip excluded containers
            if (shouldBeIgnored(cubeContainerEntry.getKey(), excludedContainers)) {
                continue;
            }

            final CubeContainer cubeContainer = cubeContainerEntry.getValue();
            if (hasMoreThanOneBindPort(cubeContainer)) {
                throw new IllegalArgumentException("No port was specified and a container has more than one bind port.");
            }

            if (hasOnlyOneBindPort(cubeContainer)) {
                if (noPreviousBindPortFound(portBindInfo)) {
                    final PortBinding portBinding = cubeContainer.getPortBindings()
                            .iterator().next();
                    int bindPort = portBinding.getBound();
                    int exposedPort = portBinding.getExposedPort().getExposed();
                    portBindInfo = new PortBindInfo(exposedPort, bindPort, cubeContainerEntry.getKey());
                } else {
                    throw new IllegalArgumentException("No port was specified and in all containers there are more than one bind port.");
                }
            }
        }

        if (noPreviousBindPortFound(portBindInfo)) {
            throw new IllegalArgumentException("There isn't any bind port.");
        }

        return portBindInfo;

    }

    private static boolean shouldBeIgnored(String containerId, String... excludedContainers) {
        for (String excludedContainer : excludedContainers) {
            if (excludedContainer.equals(containerId)) {
                return true;
            }
        }

        return false;
    }

    private static boolean noPreviousBindPortFound(PortBindInfo bindPort) {
        return bindPort == null;
    }

    private static boolean hasOnlyOneBindPort(CubeContainer cubeContainer) {

        final Collection<PortBinding> portBindings = cubeContainer.getPortBindings();
        if (portBindings == null) {
            return false;
        }

        return portBindings.size() == 1;
    }

    private static boolean hasMoreThanOneBindPort(CubeContainer cubeContainer) {

        final Collection<PortBinding> portBindings = cubeContainer.getPortBindings();
        if (portBindings == null) {
            return false;
        }

        return portBindings.size() > 1;
    }

    public static class PortBindInfo {
        private int bindPort;
        private int exposedPort;
        private String containerName;

        public PortBindInfo(int exposedPort, int bindPort, String containerName) {
            this.exposedPort = exposedPort;
            this.bindPort = bindPort;
            this.containerName = containerName;
        }

        public int getBindPort() {
            return bindPort;
        }

        public int getExposedPort() {
            return exposedPort;
        }

        public String getContainerName() {
            return containerName;
        }
    }

}
