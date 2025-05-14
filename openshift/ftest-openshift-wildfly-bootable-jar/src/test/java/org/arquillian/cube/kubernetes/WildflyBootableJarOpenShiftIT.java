package org.arquillian.cube.kubernetes;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.openshift.client.OpenShiftClient;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;

@Category(RequiresOpenshift.class)
@RequiresOpenshift
@RunWith(ArquillianConditionalRunner.class)
public class WildflyBootableJarOpenShiftIT {

    @ArquillianResource
    private OpenShiftClient openShiftClient;

    @RouteURL("hello-world-svc")
    private URL url;

    @Test
    @InSequence(0)
    public void shouldInjectValidKubernetesClient() {
        Assertions.assertThat(openShiftClient)
            .isNotNull();
    }

    @Test
    @InSequence(1)
    public void shouldFindServiceInstance() {
        final ServiceList services = openShiftClient.services().list();

        Assertions.assertThat(services.getItems())
            .hasSize(1)
            .extracting(Service::getMetadata)
            .extracting(ObjectMeta::getName)
            .containsExactlyInAnyOrder("hello-world-svc");
    }

    @Test
    @InSequence(2)
    public void helloEndpointShouldReplyWithHttp200() {
        final String serviceUrl = url.toString() + "hello";

        Awaitility.await()
            .atMost(60, TimeUnit.SECONDS)
            .until( () -> {
                Response statusResponse = RestAssured.given()
                    .when()
                    .get(serviceUrl);
                return statusResponse.statusCode() == HttpStatus.SC_OK;
            } );

        RestAssured.given()
            .when()
            .get(serviceUrl)
            .then()
            .assertThat()
            .body(is("Hello from WildFly bootable jar!"));
    }
}
