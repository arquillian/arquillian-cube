package org.arquillian.cube.impl.client;

import org.arquillian.cube.impl.model.LocalCubeRegistry;
import org.arquillian.cube.spi.CubeConfiguration;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeRegistrar {

    @Inject @ApplicationScoped
    private InstanceProducer<CubeRegistry> registryProducer;

    public void register(@Observes(precedence = 100) CubeConfiguration configuration, Injector injector) {
        CubeRegistry registry = new LocalCubeRegistry();
        registryProducer.set(registry);
    }
}
