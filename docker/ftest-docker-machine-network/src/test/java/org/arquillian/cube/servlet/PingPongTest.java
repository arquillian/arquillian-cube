package org.arquillian.cube.servlet;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.arquillian.cube.HostIp;
import org.arquillian.cube.HostPort;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.assertj.core.groups.Tuple;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

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

    @Test
    public void network_should_be_used_provided_ipam() {
        final Network app_net = dockerClient.inspectNetworkCmd().withNetworkId("app_net").exec();
        assertThat(app_net.getIpam().getDriver()).isEqualTo("default");
        assertThat(app_net.getIpam().getConfig()).extracting("subnet", "gateway")
            .contains(Tuple.tuple("172.16.238.0/24", "172.16.238.1"),
                Tuple.tuple("2001:3984:3989::/64", "2001:3984:3989::1"));
    }

    @Test
    public void network_should_be_start_with_driver_opts() {
        final Network app_net = dockerClient.inspectNetworkCmd().withNetworkId("app_net").exec();
        assertThat(app_net.getOptions()).containsEntry("com.docker.network.enable_ipv6", "true");
    }

    @Test
    public void container_should_have_static_ip_for_app_net_network() throws InterruptedException, IOException {
        final InspectContainerResponse pingpong = dockerClient.inspectContainerCmd("pingpong").exec();

        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(pingpong.getId())
            .withAttachStdout(true).withAttachStdin(true).withAttachStderr(true).withTty(false).withCmd("ifconfig")
            .exec();
        try (OutputStream outputStream = new ByteArrayOutputStream();
             OutputStream errorStream = new ByteArrayOutputStream()) {

            dockerClient.execStartCmd(execCreateCmdResponse.getId()).withDetach(false)
                .exec(new ExecStartResultCallback(outputStream, errorStream)).awaitCompletion();

            assertThat(outputStream.toString()).contains("inet addr:172.16.238.10",
                "inet6 addr: fe80::42:acff:fe10:ee0a/64");
        }
    }
}
