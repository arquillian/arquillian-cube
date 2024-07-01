package org.arquillian.cube;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;

import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@Category({ RequiresDocker.class})
@RunWith(ArquillianConditionalRunner.class)
public class StandaloneITCase {

    @HostIp
    String ip;

    @DockerUrl(containerName = "pingpong", exposedPort = 8080)
    @ArquillianResource
    private URL url;

    @Test
    @InSequence(0)
    public void shouldBeAbleToInjectController() {
        Assert.assertNotNull(ip);
    }

    @Test @InSequence(1)
    public void shouldBeAbleToCreateAndStart() throws IOException {
        String pong = ping();
        assertThat(pong, containsString("OK"));
    }

    @Test
    @InSequence(2)
    public void should_be_able_to_inject_url_in_standalone() {
        assertThat(url, is(notNullValue()));
        assertThat(url.getProtocol(), is("http"));
        assertThat(url.getHost(), is(ip));
        assertThat(url.getPort(), is(80));
    }

    private String ping() throws IOException {
        URL url = new URL("http://" + ip + ":80");
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
