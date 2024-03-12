package org.arquillian.cube.openshift.standalone;

import java.io.IOException;
import java.net.URL;

import io.restassured.RestAssured;
import org.arquillian.cube.openshift.api.Template;
import org.arquillian.cube.openshift.api.TemplateParameter;
import org.arquillian.cube.openshift.api.Templates;
import org.arquillian.cube.openshift.impl.enricher.AwaitRoute;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;


@Category(RequiresOpenshift.class)
@RequiresOpenshift
@RunWith(ArquillianConditionalRunner.class)
@Templates(templates = {
    @Template(url = "classpath:hello-openshift-templates.yaml",
        parameters = @TemplateParameter(name = "RESPONSE", value = "Hello from Arquillian Templates"))
})
public class HelloWorldTemplatesIT {

    @RouteURL("hello-openshift-templates-route")
    @AwaitRoute
    private URL helloOpenshiftTemplates;

    @Test
    public void should_create_resources_from_templates() throws IOException {
        assertThat(helloOpenshiftTemplates).isNotNull();
        RestAssured.given()
            .when()
            .get(helloOpenshiftTemplates)
            .then()
            .assertThat()
            .statusCode(200)
            .body(is("Hello from Arquillian Templates\n"));
    }
}

