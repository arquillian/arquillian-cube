package org.arquillian.cube.openshift.restassured;

import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class RestAssuredConfigurator {

    @Inject
    @ApplicationScoped
    InstanceProducer<RestAssuredConfiguration> restAssuredConfigurationInstanceProducer;

    // Need to be executed after CubeOpenShiftConfiguration
    public void configure(@Observes(precedence = -200) ArquillianDescriptor arquillianDescriptor) {
        restAssuredConfigurationInstanceProducer.set(
            RestAssuredConfiguration.fromMap(arquillianDescriptor
                .extension("restassured")
                .getExtensionProperties()));
    }
}
