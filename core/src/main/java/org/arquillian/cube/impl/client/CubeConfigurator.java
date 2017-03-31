package org.arquillian.cube.impl.client;

import java.util.Map;
import org.arquillian.cube.spi.CubeConfiguration;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeConfigurator {

    private static final String EXTENSION_NAME = "cube";

    @Inject
    @ApplicationScoped
    private InstanceProducer<CubeConfiguration> configurationProducer;

    //Add precedence -10 because we need that ContainerRegistry is available in the Arquillian scope.
    public void configure(@Observes(precedence = -10) ArquillianDescriptor arquillianDescriptor) {
        Map<String, String> config = arquillianDescriptor.extension(EXTENSION_NAME).getExtensionProperties();
        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(config);
        configurationProducer.set(cubeConfiguration);
    }
}
