package org.arquillian.cube.servlet;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Network;
import org.arquillian.cube.HostIp;
import org.arquillian.cube.HostPort;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(ArquillianConditionalRunner.class)
@RequiresDockerMachine(name = "dev")
public class PingPongTest {

    @HostIp
    String ip;

    @HostPort(containerName = "pingpong", value = 8080)
    int port;

    @ArquillianResource
    DockerClient dockerClient;

    @Test
    public void should_receive_ok_message() throws IOException {

        URL obj = new URL("http://" + ip + ":" + port);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", "Http URL");

        int responseCode = con.getResponseCode();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        assertThat(response.toString()).isEqualToIgnoringWhitespace("{  \"status\": \"OK\"}");
        
    }

    @Test
    public void container_should_be_attached_to_front_network() {
        final InspectContainerResponse pingpong = dockerClient.inspectContainerCmd("pingpong").exec();
        final Network front = dockerClient.inspectNetworkCmd().withNetworkId("front").exec();
        assertThat(front.getContainers()).containsOnlyKeys(pingpong.getId());
    }

    @Test
    public void container_should_be_attached_to_back_network() {
        final InspectContainerResponse pingpong = dockerClient.inspectContainerCmd("pingpong").exec();
        final Network front = dockerClient.inspectNetworkCmd().withNetworkId("back").exec();
        assertThat(front.getContainers()).containsOnlyKeys(pingpong.getId());
    }

}
