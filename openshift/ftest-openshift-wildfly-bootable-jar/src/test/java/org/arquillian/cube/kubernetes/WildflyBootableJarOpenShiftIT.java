package org.arquillian.cube.kubernetes;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressList;
import io.fabric8.kubernetes.api.model.networking.v1.IngressLoadBalancerIngress;
import io.fabric8.openshift.client.OpenShiftClient;
import io.restassured.RestAssured;
import org.arquillian.cube.openshift.api.OpenShiftResource;
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

import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;

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
    public void helloEndpointShouldReplyWithHttp200() throws InterruptedException {
        // wait until one route is actually ready, 60 seconds at most currently works for CI...
        Thread.sleep(60 * 1000);
        // calling the WildFly app
        final String serviceUrl = url.toString() + "hello";
        RestAssured.given()
            .when()
            .get(serviceUrl)
            .then()
            .assertThat()
            .statusCode(200)
            .body(is("Hello from WildFly bootable jar!"));
    }
}
