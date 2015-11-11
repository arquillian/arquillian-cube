package org.arquillian.cube.docker.stub;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.arquillian.cube.docker.stub.ContainerModel.Status;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import spark.utils.IOUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

public class SparkTest {

    private DockerClient docker;
    private static final SparkServer sparkServer = new SparkServer();

    @BeforeClass
    public static void startDockerStub() {
        sparkServer.start();
    }

    @Before
    public void startDockerClient() {
        DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder().withVersion("1.15")
                .withUri("http://localhost:4567").build();
        docker = DockerClientBuilder.getInstance(config).build();
    }

    @Test
    public void shouldRunAPing() {
        docker.pingCmd().exec();
    }

    @Test
    public void shouldCreateAContainer() {
        CreateContainerResponse exec = docker.createContainerCmd("tomcat").withName("tomcat")
                .withExposedPorts(ExposedPort.tcp(8080), ExposedPort.tcp(8081)).exec();
        assertThat(exec.getId(), notNullValue());
    }

    @Test
    public void shouldStartAContainer() {
        Ports ports = new Ports();
        ports.bind(ExposedPort.tcp(8080), Binding.parse("8081"));
        ports.bind(ExposedPort.tcp(8082), Binding.parse("8083"));
        CreateContainerResponse id = docker.createContainerCmd("tomcat")
                .withExposedPorts(ExposedPort.tcp(8080), ExposedPort.tcp(8081))
                .withPortBindings().exec();

        docker.startContainerCmd(id.getId()).exec();
        assertThat(sparkServer.isContainerWithOneStatus(id.getId(), Status.STARTED), is(true));
    }

    @Test
    public void shouldStopAContainer() {
        CreateContainerResponse id = docker.createContainerCmd("tomcat")
                .withExposedPorts(ExposedPort.tcp(8080), ExposedPort.tcp(8081)).exec();
        docker.stopContainerCmd(id.getId()).exec();
        assertThat(sparkServer.isContainerWithOneStatus(id.getId(), Status.STOPPED), is(true));
    }

    @Test
    public void shouldRemoveAContainer() {
        CreateContainerResponse id = docker.createContainerCmd("tomcat")
                .withExposedPorts(ExposedPort.tcp(8080), ExposedPort.tcp(8081)).exec();
        docker.stopContainerCmd(id.getId()).exec();
        docker.removeContainerCmd(id.getId()).exec();
        assertThat(sparkServer.isContainerWithOneStatus(id.getId(), Status.REMOVED), is(true));
    }

    @Test
    public void shouldWaitContainer() {
        CreateContainerResponse id = docker.createContainerCmd("tomcat")
                .withExposedPorts(ExposedPort.tcp(8080), ExposedPort.tcp(8081)).exec();
        assertThat(docker.waitContainerCmd(id.getId()).exec(), is(0));
    }

    @Test
    public void shouldCopyFileFromAContainer() throws IOException {
        Ports ports = new Ports();
        ports.bind(ExposedPort.tcp(8080), Binding.parse("8081"));
        ports.bind(ExposedPort.tcp(8082), Binding.parse("8083"));
        CreateContainerResponse id = docker.createContainerCmd("tomcat")
                .withExposedPorts(ExposedPort.tcp(8080), ExposedPort.tcp(8081))
                .withPortBindings(ports).exec();

        docker.startContainerCmd(id.getId()).exec();
        InputStream file = docker.copyFileFromContainerCmd(id.getId(), "/test").exec();
        assertThat(file.available() > 0, is(true));
    }

    @Test
    public void shouldInspectContainer() {
        Ports ports = new Ports();
        ports.bind(ExposedPort.tcp(8080), Binding.parse("8081"));
        ports.bind(ExposedPort.tcp(8082), Binding.parse("8083"));
        CreateContainerResponse id = docker.createContainerCmd("tomcat")
                .withExposedPorts(ExposedPort.tcp(8080), ExposedPort.tcp(8081))
                .withPortBindings(ports).exec();
        docker.startContainerCmd(id.getId()).exec();
        InspectContainerResponse exec = docker.inspectContainerCmd(id.getId()).exec();
        System.out.println(exec.getHostConfig().getPortBindings());
        System.out.println(exec.getId());
        System.out.println(exec.getNetworkSettings().getGateway());
    }

}
