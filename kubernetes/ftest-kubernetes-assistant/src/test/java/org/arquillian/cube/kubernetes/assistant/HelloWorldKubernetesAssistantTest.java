package org.arquillian.cube.kubernetes.assistant;

import org.arquillian.cube.kubernetes.impl.KubernetesAssistant;
import org.arquillian.cube.kubernetes.impl.requirement.RequiresKubernetes;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;

// tag::k8_assistant_example[]
@RunWith(ArquillianConditionalRunner.class)
@Category(RequiresKubernetes.class)
@RequiresKubernetes
public class HelloWorldKubernetesAssistantTest {

    @ArquillianResource
    private KubernetesAssistant kubernetesAssistant;

    @Test
    public void should_inject_kubernetes_assistant() {
        assertThat(kubernetesAssistant).isNotNull();
    }

    @Test
    public void should_apply_route_programmatically() throws IOException {
        kubernetesAssistant.deployApplication("hello-world");                   // <1>
        Optional<URL> serviceUrl = kubernetesAssistant.getServiceUrl("hello-world");    // <2>

        given()
            .when()
            .get(serviceUrl.get())
            .then()
            .assertThat()
            .statusCode(200)
            .body(is("Hello OpenShift!\n"));
    }
}
// end::k8_assistant_example[]
