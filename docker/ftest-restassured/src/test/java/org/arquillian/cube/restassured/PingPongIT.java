package org.arquillian.cube.restassured;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import java.net.MalformedURLException;

import org.apache.http.HttpStatus;
import org.arquillian.cube.DockerUrl;
import org.arquillian.cube.HealthCheck;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;

import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;

@Category({ RequiresDocker.class})

@RunWith(ArquillianConditionalRunner.class)
@HealthCheck
public class PingPongIT {

    @DockerUrl(containerName = "helloworld", exposedPort = 8080, context = "/ping")
    @ArquillianResource
    RequestSpecBuilder requestSpecBuilder;

    @Test
    public void should_receive_ok_message() throws MalformedURLException, InterruptedException {
        RestAssured
            .given()
            .spec(requestSpecBuilder.build())
            .when()
            .get()
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(is("pong"));
    }
}
