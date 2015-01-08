package org.arquillian.cube.impl.containerless;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class DaytimeTest {

    private static final String LINE_SEPARATOR = System
            .getProperty("line.separator");

    @Deployment(testable = false)
    public static JavaArchive createDeployment() {
        return ShrinkWrap
                .create(JavaArchive.class, "daytime.jar")
                .addClass(DaytimeServer.class)
                .addAsManifestResource(
                        new StringAsset(
                                "Main-Class: org.arquillian.cube.impl.containerless.DaytimeServer"
                                        + LINE_SEPARATOR), "MANIFEST.MF");
    }

    @Test
    public void shouldReturnDateFromDaytimeServer(@ArquillianResource URL base) {
        String host = base.getHost();
        int port = base.getPort();

        try (Socket echoSocket = new Socket(host, port);
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        echoSocket.getInputStream()));) {
            String userInput = in.readLine();
            assertThat(userInput, notNullValue());
        } catch (UnknownHostException e) {
            fail("Don't know about host " + host);
        } catch (IOException e) {
            fail("Couldn't get I/O for the connection to " + host);
        }

    }

}
