package org.arquillian.cube.drone;

import org.arquillian.cube.HostIp;
import org.arquillian.cube.HostPort;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.restassured.RestAssured.get;
import static org.hamcrest.CoreMatchers.equalTo;

@RunWith(Arquillian.class)
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
