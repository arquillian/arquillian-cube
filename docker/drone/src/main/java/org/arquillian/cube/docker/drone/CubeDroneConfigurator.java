package org.arquillian.cube.docker.drone;

import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeDroneConfigurator {

    @Inject
    @ApplicationScoped
    InstanceProducer<CubeDroneConfiguration> cubeDroneConfigurationInstanceProducer;

    public void configure(@Observes ArquillianDescriptor arquillianDescriptor) {
        cubeDroneConfigurationInstanceProducer.set(
            CubeDroneConfiguration.fromMap(
                arquillianDescriptor.extension("cubedrone").getExtensionProperties()
            )
        );
    }
}
