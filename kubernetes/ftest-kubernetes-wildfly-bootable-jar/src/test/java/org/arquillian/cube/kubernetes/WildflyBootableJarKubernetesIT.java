package org.arquillian.cube.kubernetes;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressList;
import io.fabric8.kubernetes.api.model.networking.v1.IngressLoadBalancerIngress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.restassured.RestAssured;
import org.arquillian.cube.kubernetes.annotations.KubernetesResource;
import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.kubernetes.annotations.PortForward;
import org.arquillian.cube.kubernetes.impl.requirement.RequiresKubernetes;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;

@Category(RequiresKubernetes.class)
@RequiresKubernetes
@RunWith(ArquillianConditionalRunner.class)
@KubernetesResource("classpath:hello-world-ingress.yaml")
public class WildflyBootableJarKubernetesIT {

    @ArquillianResource
    private KubernetesClient kubernetesClient;

    @Test
    @InSequence(0)
    public void shouldInjectValidKubernetesClient() {
        Assertions.assertThat(kubernetesClient)
            .isNotNull();
    }

    @Test
    @InSequence(1)
    public void shouldFindServiceInstance() {
        final ServiceList services = kubernetesClient.services().list();

        Assertions.assertThat(services.getItems())
            .hasSize(1)
            .extracting(Service::getMetadata)
            .extracting(ObjectMeta::getName)
            .containsExactlyInAnyOrder("hello-world-svc");
    }

    @Test
    @InSequence(2)
    public void helloEndpointShouldReplyWithHttp200() {
        final String ingressName = "hello-world-ingress";
        // An ingress has been added as well by Arquillian Cube, via the hello-world-ingress.yaml additional resource
        final IngressList ingresses = kubernetesClient.network().v1().ingresses().list();
        Assertions.assertThat(ingresses.getItems())
            .hasSize(1)
            .extracting(Ingress::getMetadata)
            .extracting(ObjectMeta::getName)
            .containsExactlyInAnyOrder(ingressName);
        // validating the ingress
        final Ingress ingress = ingresses.getItems().get(0);
        assertNotNull(ingress);
        assertNotNull(ingress.getSpec());
        assertNotNull(ingress.getSpec().getRules());
        assertFalse(ingress.getSpec().getRules().isEmpty());
        Assertions.assertThat(ingress.getSpec().getRules()).hasSize(1);
        assertNotNull(ingress.getStatus());
        assertNotNull(ingress.getStatus().getLoadBalancer());
        assertNotNull(ingress.getStatus().getLoadBalancer().getIngress());
        // wait until one ingress is actually ready, 30 seconds at most currently works for CI...
        Awaitility.await()
            .atMost(60, TimeUnit.SECONDS)
            .until(() -> kubernetesClient.network().v1().ingresses().withName(ingressName).get().getStatus().getLoadBalancer().getIngress().size() == 1);
        final IngressLoadBalancerIngress ingressLoadBalancerIngress = kubernetesClient.network().v1().ingresses()
            .withName(ingressName).get().getStatus().getLoadBalancer().getIngress().get(0);
        assertNotNull(ingressLoadBalancerIngress);
        final String ingressIp = ingressLoadBalancerIngress.getIp();
        assertNotNull(ingressIp);
        // and finally calling the WildFly app
        final String serviceUrl = String.format("http://%s/hello", ingressIp);
        RestAssured.given()
            .when()
            .get(serviceUrl)
            .then()
            .assertThat()
            .statusCode(200)
            .body(is("Hello from WildFly bootable jar!"));
    }
}
