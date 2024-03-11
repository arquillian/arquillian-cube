package org.arquillian.cube.openshift.standalone;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteList;
import io.fabric8.openshift.client.OpenShiftClient;
import java.io.IOException;
import java.net.URL;

import io.restassured.RestAssured;
import org.arquillian.cube.openshift.api.OpenShiftResource;
import org.arquillian.cube.openshift.impl.enricher.AwaitRoute;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;

@Category(RequiresOpenshift.class)
@RequiresOpenshift
@RunWith(ArquillianConditionalRunner.class)
@OpenShiftResource("classpath:hello-route.yaml")
public class HelloWorldOpenShiftResourcesIT {

    // tag::enricher_expression_resolver_example[]
    @RouteURL("${route.name}")
    @AwaitRoute
    private URL url;
    // end::enricher_expression_resolver_example[]

    @ArquillianResource
    private OpenShiftClient openShiftClient;

    @Test
    public void should_show_hello_world() throws IOException {
        assertThat(url).isNotNull();

        RestAssured.given()
            .when()
            .get(url)
            .then()
            .assertThat()
            .statusCode(200)
            .body(is("Hello OpenShift!\n"));
    }

    @Test
    @OpenShiftResource("classpath:hello-route-2.yaml")
    public void should_register_extra_route() {
        final RouteList routes = openShiftClient.routes().list();
        assertThat(routes.getItems())
            .hasSize(2)
            .extracting(Route::getMetadata)
            .extracting(ObjectMeta::getName)
            .containsExactlyInAnyOrder("hello-world", "hello-world-2");
    }

}
