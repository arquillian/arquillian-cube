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
 *
 * Usually this class is used by extensions that needs autoresolution.
 */
public class SinglePortBindResolver {

    private static final int NO_PORT = -1;

    private SinglePortBindResolver() {
        super();
    }

    /**
     * Method that tries to resolve a bind port for a given exposed port.
     * @param cubeDockerConfiguration where all docker configuration is exposed
     * @param exposedPort used to find the binding port
     * @param excludedContainers where binding port search is ignored
     * @return binding port
     */
    public static int resolveBindPort(CubeDockerConfiguration cubeDockerConfiguration, int exposedPort, String...excludedContainers) {

        final DockerCompositions dockerContainersContent = cubeDockerConfiguration.getDockerContainersContent();
        final Set<Map.Entry<String, CubeContainer>> containers = dockerContainersContent.getContainers().entrySet();

        // user specified an exposed port
        int bindPort = -1;
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
                        if (noPreviousBindPortFound(bindPort)) {
                            bindPort = portBinding.getBound();
                        } else {
                            throw new IllegalArgumentException(String.format("More than one docker container with port binding having exposed port %s.", exposedPort));
                        }
                    }
                }
            }

            if (noPreviousBindPortFound(bindPort)) {
                return exposedPort;
            }
        }
        return bindPort;
    }

    /**
     * Method that tries to resolve a bind port by searching if there is only one binding port across all running containers
     * @param cubeDockerConfiguration where all docker configuration is exposed
     * @param excludedContainers where binding port search is ignored
     * @return binding port
     */
    public static int resolveBindPort(CubeDockerConfiguration cubeDockerConfiguration, String...excludedContainers) {

        final DockerCompositions dockerContainersContent = cubeDockerConfiguration.getDockerContainersContent();
        final Set<Map.Entry<String, CubeContainer>> containers = dockerContainersContent.getContainers().entrySet();

            //if no port, we check if there is only one cube with one bind port and if not, return default one

            int bindPort = -1;
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
                    if (noPreviousBindPortFound(bindPort)) {
                        bindPort = cubeContainer.getPortBindings()
                                .iterator().next()
                                .getBound();
                    } else {
                        throw new IllegalArgumentException("No port was specified and in all containers there are more than one bind port.");
                    }
                }
            }

            if (noPreviousBindPortFound(bindPort)) {
                throw new IllegalArgumentException("There isn't any bind port.");
            }

            return bindPort;

    }

    private static boolean shouldBeIgnored(String containerId, String...excludedContainers) {
        for (String excludedContainer : excludedContainers) {
            if (excludedContainer.equals(containerId)) {
                return true;
            }
        }

        return false;
    }

    private static boolean noPreviousBindPortFound(int bindPort) {
        return bindPort == -1;
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

}
