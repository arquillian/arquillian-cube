package org.arquillian.cube;

import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.junit.InSequence;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

@RunWith(ArquillianConditionalRunner.class)
@RequiresDockerMachine(name = "dev")
public class StandaloneTestCase {

    @HostIp
    String ip;

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

    private String ping() throws IOException {
        URL url = new URL("http://" + ip + ":8080");
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
