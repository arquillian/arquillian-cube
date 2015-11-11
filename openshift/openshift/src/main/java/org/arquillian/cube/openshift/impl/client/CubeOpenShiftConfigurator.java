package org.arquillian.cube.openshift.impl.client;

import java.util.Map;

import org.arquillian.cube.spi.CubeConfiguration;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeOpenShiftConfigurator {

    private static final String EXTENSION_NAME = "openshift";

    @Inject
    @ApplicationScoped
    private InstanceProducer<CubeOpenShiftConfiguration> configurationProducer;

    public void configure(@Observes CubeConfiguration event, ArquillianDescriptor arquillianDescriptor) {
        Map<String, String> config = arquillianDescriptor.extension(EXTENSION_NAME).getExtensionProperties();
        CubeOpenShiftConfiguration cubeConfiguration = CubeOpenShiftConfiguration.fromMap(config);
        configurationProducer.set(cubeConfiguration);
    }
}
