package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.model.LocalDockerNetworkRegistry;
import org.arquillian.cube.docker.impl.model.NetworkRegistry;
import org.arquillian.cube.spi.CubeConfiguration;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class NetworkRegistrar {

    @Inject @ApplicationScoped
    private InstanceProducer<NetworkRegistry> networkRegistryProducer;

    public void register(@Observes(precedence = 100) CubeConfiguration configuration, Injector injector) {
        NetworkRegistry registry = new LocalDockerNetworkRegistry();
        networkRegistryProducer.set(registry);
    }
}
