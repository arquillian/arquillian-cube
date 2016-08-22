package org.arquillian.cube.restassured;

import io.restassured.RestAssured;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;

import static org.hamcrest.CoreMatchers.equalTo;

@RunWith(ArquillianConditionalRunner.class)
@RequiresDockerMachine(name = "dev")
public class PingPongTest {

    @Test
    public void should_receive_ok_message() throws MalformedURLException, InterruptedException {
        RestAssured.get().then().assertThat().body("status", equalTo("OK"));
    }

}
