package org.arquillian.cube.containerobject;

import com.github.dockerjava.api.DockerClient;
import org.arquillian.cube.HostIp;
import org.jboss.arquillian.test.api.ArquillianResource;

@CubeDockerFile(remove = false, nocache = false)
public class SshdContainer {

    private final String user = "myuser";
    private final String password = "myverysecretpassword";

    @HostIp
    private String ip;

    @ArquillianResource
    private DockerClient dockerClient;

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getIp() {
        return ip;
    }

    public DockerClient getDockerClient() {
        return dockerClient;
    }
}
