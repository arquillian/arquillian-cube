package org.arquillian.cube.docker.impl.client.containerobject.dsl;

import org.arquillian.cube.HostIpContext;
import org.arquillian.cube.containerobject.ConnectionMode;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

/**
 * Base class representing a container.
 */
public class Container {

    @Inject
    Instance<HostIpContext> hostIpContextInstance;

    @Inject
    Instance<CubeRegistry> cubeRegistryInstance;

    private String containerName;
    private CubeContainer cubeContainer;
    private ConnectionMode connectionMode;

    protected Container(String containerName, CubeContainer cubeContainer, ConnectionMode connectionMode) {
        this.containerName = containerName;
        this.cubeContainer = cubeContainer;
        this.connectionMode = connectionMode;
    }

    public static ContainerBuilder withContainerName(String containerName) {
        return ContainerBuilder.newContainer(containerName);
    }

    public String getContainerName() {
        return containerName;
    }

    public CubeContainer getCubeContainer() {
        return cubeContainer;
    }

    public ConnectionMode getConnectionMode() {
        return connectionMode;
    }

    /**
     * Returns the ip where this container can be reachable from outside docker host.
     *
     * @return Ip.
     */
    public String getIpAddress() {
        return hostIpContextInstance.get().getHost();
    }

    /**
     * Returns binding port for given exposed port.
     *
     * @param exposedPort
     *     to resolve.
     *
     * @return Binding port.
     */
    public int getBindPort(int exposedPort) {
        return getBindingPort(this.containerName, exposedPort);
    }

    private int getBindingPort(String cubeId, int exposedPort) {

        int bindPort = -1;

        final Cube cube = getCube(cubeId);

        if (cube != null) {
            final HasPortBindings portBindings = (HasPortBindings) cube.getMetadata(HasPortBindings.class);
            final HasPortBindings.PortAddress mappedAddress = portBindings.getMappedAddress(exposedPort);

            if (mappedAddress != null) {
                bindPort = mappedAddress.getPort();
            }
        }

        return bindPort;
    }

    private Cube getCube(String cubeId) {
        return cubeRegistryInstance.get().getCube(cubeId);
    }
}
