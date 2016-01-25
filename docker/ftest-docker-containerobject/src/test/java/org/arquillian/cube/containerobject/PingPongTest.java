package org.arquillian.cube.containerobject;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Arquillian.class)
public class PingPongTest {

    @Cube
    PingPongContainer pingPongContainer;

    @Test
    public void shouldReturnOkAsPong() throws IOException {
        String pong = ping();
        assertThat(pong, containsString("OK"));
        assertThat(pingPongContainer.getConnectionPort(), is(5000));
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
