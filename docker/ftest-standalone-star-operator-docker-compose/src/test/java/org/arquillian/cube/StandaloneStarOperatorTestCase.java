package org.arquillian.cube;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@Category(RequiresDockerMachine.class)
@RequiresDockerMachine(name = "dev")
@RunWith(ArquillianConditionalRunner.class)
public class StandaloneStarOperatorTestCase {

    @HostIp
    String ip;

    @HostPort(containerName = "pingpong*", value = 8080)
    int port;

    @DockerUrl(containerName = "pingpong*", exposedPort = 8080)
    @ArquillianResource
    private URL url;

    @Test
    public void shouldHaveRandomizedPort() {
        Assert.assertNotEquals(8080, port);
    }

    @Test
    public void shouldHaveRandomizedUrlPort() {
        assertThat(url, is(notNullValue()));
        assertThat(url.getProtocol(), is("http"));
        assertThat(url.getHost(), is(ip));
        assertThat(url.getPort(), is(not(8080)));
        assertThat(url.getPort(), is(port));
    }

    @Test
    public void shouldBeAvailable() throws IOException {
        String pong = ping();
        assertThat(pong, containsString("OK"));
    }

    private String ping() throws IOException {
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
