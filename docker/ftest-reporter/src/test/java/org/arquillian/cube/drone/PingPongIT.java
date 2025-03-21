package org.arquillian.cube.drone;

import org.apache.http.HttpStatus;
import org.arquillian.cube.HostIp;
import org.arquillian.cube.HostPort;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;

import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static io.restassured.RestAssured.get;
import static org.hamcrest.CoreMatchers.is;

@Category({ RequiresDocker.class})

@RunWith(ArquillianConditionalRunner.class)
public class PingPongIT {

    @HostIp
    String ip;

    @HostPort(containerName = "pingpong", value = 8080)
    int port;

    @Test
    public void shouldDoPingPong() {
        get("http://" + ip + ":" + port + "/ping").then().statusCode(HttpStatus.SC_OK).body(is("pong"));
    }
}
