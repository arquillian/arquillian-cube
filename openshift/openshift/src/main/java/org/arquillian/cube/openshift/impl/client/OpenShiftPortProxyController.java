package org.arquillian.cube.openshift.impl.client;

import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.AfterStart;
import org.arquillian.cube.spi.event.lifecycle.BeforeStop;
import org.arquillian.cube.spi.metadata.HasMappedPorts;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.core.api.annotation.Observes;

/**
 * OpenShiftPortProxyController
 * <p/>
 * Manages creation of port proxies to container ports.
 * 
 * @author Rob Cernich
 */
public class OpenShiftPortProxyController {

    public void createProxies(@Observes AfterStart event, CubeOpenShiftConfiguration openshiftConfiguration,
            CubeRegistry cubeRegistry, ContainerRegistry containerRegistry) throws InstantiationException,
            IllegalAccessException, Exception {
        Cube<?> cube = cubeRegistry.getCube(event.getCubeId());
        if (cube == null) {
            return; // No Cube found matching Container name, not managed by
                    // Cube
        }
        HasMappedPorts mappedPorts = cube.getMetadata(HasMappedPorts.class);
        if (mappedPorts == null) {
            return;
        }
        mappedPorts.createProxies();
    }

    public void destroyProxies(@Observes BeforeStop event, CubeRegistry cubeRegistry) {
        Cube<?> cube = cubeRegistry.getCube(event.getCubeId());
        if (cube == null) {
            return; // No Cube found matching Container name, not managed by
                    // Cube
        }
        HasMappedPorts mappedPorts = cube.getMetadata(HasMappedPorts.class);
        if (mappedPorts == null) {
            return;
        }
        mappedPorts.destroyProxies();
    }
}
