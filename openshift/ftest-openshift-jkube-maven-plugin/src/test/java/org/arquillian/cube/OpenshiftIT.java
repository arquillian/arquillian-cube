package org.arquillian.cube;

import io.restassured.RestAssured;
import java.net.URL;
import org.arquillian.cube.openshift.impl.enricher.AwaitRoute;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;
import static io.restassured.RestAssured.given;


@Category(RequiresOpenshift.class)
@RequiresOpenshift
@RunWith(ArquillianConditionalRunner.class)
public class OpenshiftIT {

    @AwaitRoute
    @RouteURL("${app.name}")
    private URL baseURL;

    @Before
    public void setup() throws Exception {
        RestAssured.baseURI = baseURL.toString();
    }

    @Test
    public void testGreetingEndpoint() {
            when()
                .get()
            .then()
                .statusCode(200)
                .body(containsString("Greetings from Spring Boot!"));
    }
}
