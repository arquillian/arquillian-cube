package org.arquillian.cube.openshift.standalone;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.client.OpenShiftClient;
import java.io.IOException;
import java.net.URL;

import io.restassured.RestAssured;
import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.kubernetes.annotations.PortForward;
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
public class HelloWorldIT {

    @Named("hello-openshift-service")
    @PortForward
    @ArquillianResource
    private Service service;

    @Named("hello-openshift-service")
    @PortForward
    @ArquillianResource
    private URL url;

    @ArquillianResource
    private OpenShiftClient client;

    @Test
    public void client_should_not_be_null() throws IOException {
        assertThat(client).isNotNull();
    }

    @Test
    public void service_instance_should_not_be_null() throws IOException {
        assertThat(service).isNotNull();
        assertThat(service.getSpec()).isNotNull();
        assertThat(service.getSpec().getPorts()).isNotNull();
        assertThat(service.getSpec().getPorts()).isNotEmpty();
    }

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
}
