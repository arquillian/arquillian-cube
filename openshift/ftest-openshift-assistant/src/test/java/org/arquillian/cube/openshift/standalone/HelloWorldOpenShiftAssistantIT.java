package org.arquillian.cube.openshift.standalone;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.client.readiness.Readiness;
import io.restassured.RestAssured;
import org.arquillian.cube.openshift.impl.client.OpenShiftAssistant;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.fabric8.kubernetes.api.model.Pod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;

@Category(RequiresOpenshift.class)
@RequiresOpenshift
@RunWith(ArquillianConditionalRunner.class)
public class HelloWorldOpenShiftAssistantIT {

    @ArquillianResource
    private OpenShiftAssistant openShiftAssistant;

    @Test
    public void should_inject_openshift_assistant() {
        assertThat(openShiftAssistant).isNotNull();
    }

    @Test
    public void should_deploy_app_programmatically() throws IOException {
        openShiftAssistant.deployApplication("hello-openshift-deployment-config", "deployment.yml");

        openShiftAssistant.awaitApplicationReadinessOrFail();

        List<Pod> pods = openShiftAssistant.getClient()
            .pods()
            .inNamespace(openShiftAssistant.getCurrentProjectName())
            .withLabel("name", "hello-openshift-deployment-config")
            .list()
            .getItems();
        assertThat(pods.size()).isGreaterThan(1);
        assertThat(pods).allMatch(Readiness::isPodReady);
    }

    @Test
    public void should_apply_route_programmatically() throws IOException {

        openShiftAssistant.deployApplication("hello-world", "hello-route.json");

        final Optional<URL> route = openShiftAssistant.getRoute();
        openShiftAssistant.awaitUrl(route.get());

        RestAssured.given()
            .when()
            .get(route.get())
            .then()
            .assertThat()
            .statusCode(200)
            .body(is("Hello OpenShift!\n"));
    }
}
