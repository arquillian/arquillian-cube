package org.arquillian.cube.openshift.impl.client;

import java.util.Set;

import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.Binding.PortBinding;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.AfterStart;
import org.arquillian.cube.spi.event.lifecycle.BeforeStop;
import org.arquillian.cube.spi.metadata.CanForwardPorts;
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
        CanForwardPorts forwardPorts = cube.getMetadata(CanForwardPorts.class);
        if (forwardPorts == null) {
            return;
        }

        Set<String> proxiedPorts = openshiftConfiguration.getProxiedContainerPorts();
        Binding binding = cube.configuredBindings();
        if (binding.arePortBindings()) {
            for (PortBinding portBinding : binding.getPortBindings()) {
                if (proxiedPorts
                        .contains(String.format("%s:%d", cube.getId(), portBinding.getBindingPort()))) {
                    forwardPorts.createProxy(portBinding);
                }
            }
        }
    }

    public void destroyProxies(@Observes BeforeStop event, CubeRegistry cubeRegistry) {
        Cube<?> cube = cubeRegistry.getCube(event.getCubeId());
        if (cube == null) {
            return; // No Cube found matching Container name, not managed by
                    // Cube
        }
        CanForwardPorts forwardPorts = cube.getMetadata(CanForwardPorts.class);
        if (forwardPorts == null) {
            return;
        }
        forwardPorts.destroyProxies();
    }
}
