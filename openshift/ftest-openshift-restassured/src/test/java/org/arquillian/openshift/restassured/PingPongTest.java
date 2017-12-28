package org.arquillian.openshift.restassured;

import io.restassured.builder.RequestSpecBuilder;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class PingPongTest {

    @ArquillianResource
    RequestSpecBuilder requestSpecBuilder;

    @Test
    public void testGreetingEndpoint() {
        given(requestSpecBuilder.build())
            .when().get()
            .then()
            .statusCode(200)
            .body(containsString("Hello OpenShift!"));
    }
}
