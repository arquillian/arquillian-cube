package org.arquillian.cube.containerobject;

import org.arquillian.cube.HostIp;
import org.arquillian.cube.HostPort;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.docker.DockerDescriptor;

@Cube(value = "pingpong", portBinding = "5000->8080/tcp")
public class PingPongContainer {

    @HostIp
    String dockerHost;

    @HostPort(8080)
    private int port;

    @CubeDockerFile
    public static Archive<?> createContainer() {
        String dockerDescriptor = Descriptors.create(DockerDescriptor.class)
            .from("hashicorp/http-echo")
            .expose(8080)
            .cmd("-listen=:8080")
            .exportAsString();
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
