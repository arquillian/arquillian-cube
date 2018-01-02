package org.arquillian.cube.openshift.restassured;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.api.event.ManagerStopping;


/**
 * Class that gets restassured configuration and configures the RestAssured instance accordantly
 */
public class RestAssuredCustomizer {

    @Inject
    private Instance<Configuration> configurationInstance;

    @Inject
    @ApplicationScoped
    InstanceProducer<RequestSpecBuilder> requestSpecBuilderInstanceProducer;

    public void configure(@Observes RestAssuredConfiguration restAssuredConfiguration) {

        final CubeOpenShiftConfiguration openShiftConfiguration = (CubeOpenShiftConfiguration) configurationInstance.get();

        final RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();

        configureRequestSpecBuilder(restAssuredConfiguration, openShiftConfiguration, requestSpecBuilder);

        requestSpecBuilderInstanceProducer.set(requestSpecBuilder);
    }

    void configureRequestSpecBuilder(@Observes RestAssuredConfiguration restAssuredConfiguration, CubeOpenShiftConfiguration openShiftConfiguration, RequestSpecBuilder requestSpecBuilder) {
        if (restAssuredConfiguration.isBaseUriSet()) {
            requestSpecBuilder.setBaseUri(restAssuredConfiguration.getBaseUri());
        } else {
            requestSpecBuilder.setBaseUri(
                restAssuredConfiguration.getSchema() + "://" + openShiftConfiguration.getMasterUrl());
        }

        if (restAssuredConfiguration.isPortSet()) {
            requestSpecBuilder.setPort(restAssuredConfiguration.getPort());
        } else {
            requestSpecBuilder.setPort(openShiftConfiguration.getOpenshiftRouterHttpPort());
        }

        if (restAssuredConfiguration.isBasePathSet()) {
            requestSpecBuilder.setBasePath(restAssuredConfiguration.getBasePath());
        }

        if (restAssuredConfiguration.isAuthenticationSchemeSet()) {
            requestSpecBuilder.setAuth(restAssuredConfiguration.getAuthenticationScheme());
        }

        if (restAssuredConfiguration.isUseRelaxedHttpsValidationSet()) {
            if (restAssuredConfiguration.isUseRelaxedHttpsValidationInAllProtocols()) {
                requestSpecBuilder.setRelaxedHTTPSValidation();
            } else {
                requestSpecBuilder.setRelaxedHTTPSValidation(restAssuredConfiguration.getUseRelaxedHttpsValidation());
            }
        }
    }

    /**
     * Resets RestAssured configuration values to default.
     */
    public void resetRestAssuredConfiguration(@Observes ManagerStopping event) {
        RestAssured.reset();
    }
}
