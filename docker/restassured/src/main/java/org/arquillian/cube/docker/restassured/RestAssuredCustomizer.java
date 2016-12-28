package org.arquillian.cube.docker.restassured;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.util.SinglePortBindResolver;
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
    Instance<CubeDockerConfiguration> cubeDockerConfigurationInstance;

    @Inject
    @ApplicationScoped
    InstanceProducer<RequestSpecBuilder> requestSpecBuilderInstanceProducer;

    /**
     * Method executed before starting a test.
     * It is important to do it in this event because in case of incontainer tests or containerless,
     * this is when the mapped container is started and you might need to inspect the automatic port binding.
     *
     * Precedence is set to -100 to execute this after all sarting events.
     *
     * @param restAssuredConfiguration
     */
    public void configure(@Observes RestAssuredConfiguration restAssuredConfiguration) {

        CubeDockerConfiguration cubeDockerConfiguration = cubeDockerConfigurationInstance.get();
        final RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();

        configureRequestSpecBuilder(restAssuredConfiguration, cubeDockerConfiguration, requestSpecBuilder);

        requestSpecBuilderInstanceProducer.set(requestSpecBuilder);

    }

    void configureRequestSpecBuilder(@Observes RestAssuredConfiguration restAssuredConfiguration, CubeDockerConfiguration cubeDockerConfiguration, RequestSpecBuilder requestSpecBuilder) {
        if (restAssuredConfiguration.isBaseUriSet()) {
            requestSpecBuilder.setBaseUri(restAssuredConfiguration.getBaseUri());
        } else {
            requestSpecBuilder.setBaseUri(restAssuredConfiguration.getSchema() + "://" + cubeDockerConfiguration.getDockerServerIp());
        }

        if (restAssuredConfiguration.isPortSet()) {
            requestSpecBuilder.setPort(SinglePortBindResolver.resolveBindPort(cubeDockerConfiguration,
                    restAssuredConfiguration.getPort(),
                    restAssuredConfiguration.getExclusionContainers()));
        } else {
            requestSpecBuilder.setPort(SinglePortBindResolver.resolveBindPort(cubeDockerConfiguration,
                    restAssuredConfiguration.getExclusionContainers()));
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
     * @param event
     */
    public void resetRestAssuredConfiguration(@Observes ManagerStopping event) {
        RestAssured.reset();
    }

}
