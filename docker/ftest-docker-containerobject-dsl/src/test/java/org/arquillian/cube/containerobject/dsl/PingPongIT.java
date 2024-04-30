package org.arquillian.cube.containerobject.dsl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.Container;
import org.arquillian.cube.docker.impl.client.containerobject.dsl.DockerContainer;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@Category({RequiresDockerMachine.class, RequiresDocker.class})
@RequiresDockerMachine(name = "dev")
@RunWith(ArquillianConditionalRunner.class)
public class PingPongIT {

    @DockerContainer
    Container pingpong = Container.withContainerName("pingpong")
        .fromImage("hashicorp/http-echo")
        .withPortBinding(8080)
        .withCommand("-listen=:8080", "-text=OK")
        .build();

    @Test
    public void should_return_ok_as_pong() throws IOException {
        String response = ping(pingpong.getIpAddress(), pingpong.getBindPort(8080));
        assertThat(response).containsSequence("OK");
    }

    public String ping(String ip, int port) throws IOException {
        URL url = new URL("http://" + ip + ":" + port);
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
