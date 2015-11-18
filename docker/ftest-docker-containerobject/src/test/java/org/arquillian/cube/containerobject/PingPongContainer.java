package org.arquillian.cube.containerobject;

import org.arquillian.cube.HostIp;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.docker.DockerDescriptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

@Cube(value = "pingpong", portBinding = "5000->8080/tcp")
public class PingPongContainer {

    @HostIp
    String dockerHost;

    @HostPort(8080)
    private int port;

    @CubeDockerFile
    public static Archive<?> createContainer() {
        String dockerDescriptor = Descriptors.create(DockerDescriptor.class).from("jonmorehouse/ping-pong").exportAsString();
        return ShrinkWrap.create(GenericArchive.class)
                .add(new StringAsset(dockerDescriptor), "Dockerfile");
    }

    public int getConnectionPort() {
        return port;
    }

    public String getDockerHost() {
        return this.dockerHost;
    }
}
