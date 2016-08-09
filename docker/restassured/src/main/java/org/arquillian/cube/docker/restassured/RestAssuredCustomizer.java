package org.arquillian.cube.docker.restassured;

import io.restassured.RestAssured;
import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.util.SinglePortBindResolver;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.api.event.ManagerStopping;

/**
 * Class that gets restassured configuration and configures the RestAssured instance accordantly
 */
public class RestAssuredCustomizer {

    @Inject
    Instance<CubeDockerConfiguration> cubeDockerConfigurationInstance;

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

        if (restAssuredConfiguration.isBaseUriSet()) {
            RestAssured.baseURI = restAssuredConfiguration.getBaseUri();
        } else {
            RestAssured.baseURI = restAssuredConfiguration.getSchema() + "://" + cubeDockerConfiguration.getDockerServerIp();
        }

        if (restAssuredConfiguration.isPortSet()) {
            RestAssured.port = SinglePortBindResolver.resolveBindPort(cubeDockerConfiguration,
                    restAssuredConfiguration.getPort(),
                    restAssuredConfiguration.getExclusionContainers());
        } else {
            RestAssured.port = SinglePortBindResolver.resolveBindPort(cubeDockerConfiguration,
                    restAssuredConfiguration.getExclusionContainers());
        }

        if (restAssuredConfiguration.isBasePathSet()) {
            RestAssured.basePath = restAssuredConfiguration.getBasePath();
        }

        if (restAssuredConfiguration.isAuthenticationSchemeSet()) {
            RestAssured.authentication = restAssuredConfiguration.getAuthenticationScheme();
        }

        if (restAssuredConfiguration.isRootPath()) {
            RestAssured.rootPath = restAssuredConfiguration.getRootPath();
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
