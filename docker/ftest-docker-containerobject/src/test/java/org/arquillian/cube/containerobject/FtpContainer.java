package org.arquillian.cube.containerobject;

import com.github.dockerjava.api.DockerClient;
import java.io.InputStream;
import org.arquillian.cube.HostIp;
import org.jboss.arquillian.test.api.ArquillianResource;

@Cube(value = "ftp",
    portBinding = {FtpContainer.BIND_PORT + "->21/tcp",
        "37185->37185/tcp", "37186->37186/tcp", "37187->37187/tcp",
        "37188->37188/tcp", "37189->37189/tcp", "37190->37190/tcp"},
    awaitPorts = 21)
@Image("dipakpawar231/proftpd:v1")
@Environment(key = "USERNAME", value = FtpContainer.USERNAME)
@Environment(key = "PASSWORD", value = FtpContainer.PASSWORD)
public class FtpContainer {

    static final String USERNAME = "alex";
    static final String PASSWORD = "aixa";
    static final int BIND_PORT = 2121;

    @ArquillianResource
    DockerClient dockerClient;

    @HostIp
    String ip;

    public String getIp() {
        return ip;
    }

    public String getUsername() {
        return USERNAME;
    }

    public String getPassword() {
        return PASSWORD;
    }

    public int getBindPort() {
        return BIND_PORT;
    }

    public boolean isFilePresentInContainer(String filename) {
        try (
            final InputStream file = dockerClient.copyArchiveFromContainerCmd("ftp",
                "/ftp/" + filename).exec()) {
            return file != null;
        } catch (Exception e) {
            return false;
        }
    }
}
