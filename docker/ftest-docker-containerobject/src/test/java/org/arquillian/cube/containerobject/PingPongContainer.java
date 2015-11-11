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

    /**
     * DockerDescriptor descriptor =
     Descriptors.create(DockerDescriptor.class)
     .from("jbossforge")
     .user("George");
     */
    // docker pull jonmorehouse/ping-pong
    /**
     * docker run -p 5000:8080 -d jonmorehouse/ping-pong

     $ curl localhost:5000

     {
     "status": "ok"
     }
     */

    @HostIp
    String dockerHost;

    @CubeDockerFile
    public static Archive<?> createContainer() {
        String dockerDescriptor = Descriptors.create(DockerDescriptor.class).from("jonmorehouse/ping-pong").exportAsString();
        return ShrinkWrap.create(GenericArchive.class)
                .add(new StringAsset(dockerDescriptor), "Dockerfile");
    }

    public int getConnectionPort() {
        return 5000;
    }

    public String getDockerHost() {
        return this.dockerHost;
    }
}
