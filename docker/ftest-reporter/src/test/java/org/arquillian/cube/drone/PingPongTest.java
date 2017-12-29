package org.arquillian.cube.drone;

import org.arquillian.cube.HostIp;
import org.arquillian.cube.HostPort;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static io.restassured.RestAssured.get;
import static org.hamcrest.CoreMatchers.equalTo;

@Category(RequiresDockerMachine.class)
@RequiresDockerMachine(name = "dev")
@RunWith(ArquillianConditionalRunner.class)
public class PingPongTest {

    @HostIp
    String ip;

    @HostPort(containerName = "pingpong", value = 8080)
    int port;

    @Test
    public void shouldDoPingPong() {
        get("http://" + ip + ":" + port).then().assertThat().body("status", equalTo("OK"));
    }
}
