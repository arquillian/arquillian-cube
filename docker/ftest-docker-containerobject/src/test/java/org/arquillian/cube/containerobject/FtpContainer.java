package org.arquillian.cube.containerobject;

import com.github.dockerjava.api.DockerClient;
import java.io.InputStream;
import org.arquillian.cube.HostIp;
import org.jboss.arquillian.test.api.ArquillianResource;

@Cube(value = "ftp",
    portBinding = {FtpContainer.BIND_PORT + "->21/tcp",
        "21000->21000/tcp", "21001->21001/tcp", "21002->21002/tcp", "21003->21003/tcp", "21004->21004/tcp", "21005->21005/tcp",
        "21006->21006/tcp", "21007->21007/tcp", "21008->21008/tcp", "21009->21009/tcp", "21010->21010/tcp"},
    awaitPorts = 21)
@Image("delfer/alpine-ftp-server")
@Environment(key = "USERS", value = FtpContainer.USERNAME + "|" + FtpContainer.PASSWORD)
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
                "/ftp/" + getUsername() + "/" + filename).exec()) {
            return file != null;
        } catch (Exception e) {
            return false;
        }
    }
}
