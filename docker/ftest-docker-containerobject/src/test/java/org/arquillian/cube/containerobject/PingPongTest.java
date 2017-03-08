package org.arquillian.cube.containerobject;

import org.arquillian.cube.CubeIp;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(ArquillianConditionalRunner.class)
@RequiresDockerMachine(name = "dev")
public class PingPongTest {

    @Cube
    PingPongContainer pingPongContainer;

    @CubeIp(containerName = "pingpong")
    private String pingpongAddr;

    @Test
    public void shouldReturnOkAsPong() throws IOException {
        String pong = ping();
        assertThat(pong, containsString("OK"));
        assertThat(pingPongContainer.getConnectionPort(), is(5000));
    }

    @Test
    public void shouldInjectCubeIp() {
        assertThat(pingpongAddr, notNullValue());
    }

    public String ping() throws IOException {
        URL url = new URL("http://" + pingPongContainer.getDockerHost() + ":" + pingPongContainer.getConnectionPort());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

}
