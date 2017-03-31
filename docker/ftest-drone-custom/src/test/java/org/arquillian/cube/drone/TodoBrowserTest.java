package org.arquillian.cube.drone;

import java.net.MalformedURLException;
import java.net.URL;
import org.arquillian.cube.CubeIp;
import org.arquillian.cube.HostPort;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(ArquillianConditionalRunner.class)
@RequiresDockerMachine(name = "dev")
public class TodoBrowserTest {

    @Drone
    WebDriver webDriver;

    @CubeIp(containerName = "helloworld")
    String ip;

    @HostPort(containerName = "helloworld", value = 80)
    int port;

    @Test
    public void shouldShowHelloWorld() throws MalformedURLException, InterruptedException {
        URL url = new URL("http", ip, port, "/");
        webDriver.get(url.toString());
        final String message = webDriver.findElement(By.tagName("h1")).getText();
        assertThat(message, is("Hello world!"));
    }
}
