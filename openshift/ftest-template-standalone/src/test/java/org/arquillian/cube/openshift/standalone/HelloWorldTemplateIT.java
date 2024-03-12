package org.arquillian.cube.openshift.standalone;

import java.io.IOException;
import java.net.URL;

import io.restassured.RestAssured;
import org.arquillian.cube.openshift.api.Template;
import org.arquillian.cube.openshift.api.TemplateParameter;
import org.arquillian.cube.openshift.impl.enricher.AwaitRoute;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;

// tag::openshift_template_example[]
@Category(RequiresOpenshift.class)
@RequiresOpenshift
@RunWith(ArquillianConditionalRunner.class)
@Template(url = "classpath:hello-openshift.yaml",
          parameters = @TemplateParameter(name = "RESPONSE", value = "Hello from Arquillian Template"))
public class HelloWorldTemplateIT {

    @RouteURL("hello-openshift-route")
    @AwaitRoute
    private URL url;

    @Test
    public void should_create_class_template_resources() throws IOException {
        verifyResponse(url);
    }

    @Test
    @Template(url = "https://gist.githubusercontent.com/dipak-pawar/403b870fc92f6569f64f12b506318606/raw/4dd7cd4b259f893353509411ba4777792cacd034/hello_openshift_route_template.yaml",
        parameters = @TemplateParameter(name = "ROUTE_NAME", value = "hello-openshift-method-route"))
    public void should_create_method_template_resources(
        @RouteURL("hello-openshift-method-route") @AwaitRoute URL routeUrl)
        throws IOException {
        verifyResponse(routeUrl);
    }

    private void verifyResponse(URL url) throws IOException {
        assertThat(url).isNotNull();

        RestAssured.given()
            .when()
            .get(url)
            .then()
            .assertThat()
            .statusCode(200)
            .body(is("Hello from Arquillian Template\n"));
    }
}
// end::openshift_template_example[]
