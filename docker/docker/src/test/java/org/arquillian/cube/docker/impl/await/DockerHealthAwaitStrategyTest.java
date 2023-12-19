package org.arquillian.cube.docker.impl.await;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Cube;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

//@RunWith(MockitoJUnitRunner.class)
public class DockerHealthAwaitStrategyTest {

    private static final String DOCKER_HOST = "DOCKER_HOST";

    private static DockerClient dockerClient;
    private static String healthSuccessImageId;
    private static String healthFailureImageId;

    @ClassRule
    public static final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Mock
    private DockerClientExecutor dockerClientExecutor;

    @Mock
    private Cube<?> cube;

    private List<String> containerIds;

    @BeforeClass
    public static void createDockerClient() {
        if (!System.getenv().containsKey(DOCKER_HOST) || System.getenv(DOCKER_HOST).equals("")){
            environmentVariables.set(DOCKER_HOST, "unix:///var/run/podman.sock");
        }
        dockerClient = DockerClientBuilder.getInstance().build();
        healthSuccessImageId = dockerBuild("DockerHealthAwait/HealthSuccess/Dockerfile");
        healthFailureImageId = dockerBuild("DockerHealthAwait/HealthFailure/Dockerfile");
    }

    @Before
    public void setup() {
        containerIds = new ArrayList<>();
    }

    @After
    public void cleanContainers() {
        for (String containerId : containerIds) {
            if (containerId != null) {
                dockerRm(containerId);
            }
        }
    }

    @AfterClass
    public static void cleanImages() {
        dockerRmi(healthSuccessImageId);
        dockerRmi(healthFailureImageId);
    }

    //@Test
    public void shouldSuccessHealthCheck() {
        verifyDockerHealthCheckResult(healthSuccessImageId, true);
    }

    //@Test
    public void shouldFailHealthcheck() {
        verifyDockerHealthCheckResult(healthFailureImageId, false);
    }

    //@Test
    public void shouldSuccessCustomExec() {
        verifyAwait(healthFailureImageId, true, new String[] {"true"});
    }

    //@Test
    public void shouldSuccessWithLongCommands() {
        verifyAwait(healthFailureImageId, true, new String[] {"sh", "-c", "echo start;sleep 2;echo stop"}, "3s");
    }

    //@Test
    public void shouldFailCustomExecFailed() {
        verifyAwait(healthSuccessImageId, false, new String[] {"sh", "-c", "exit 1"});
    }

    //@Test
    public void shouldFailNonExistingCommand() {
        verifyAwait(healthSuccessImageId, false, new String[] {"this command does not exist"});
    }

    private static String dockerBuild(String path) {
        URL url = DockerHealthAwaitStrategyTest.class.getClassLoader().getResource(path);
        return dockerClient.buildImageCmd(new File(url.getFile()).getParentFile())
            .exec(new BuildImageResultCallback()).awaitImageId();
    }

    private static void dockerRmi(String imageId) {
        if (imageId != null) {
            try {
                dockerClient.removeImageCmd(imageId).exec();
                System.out.printf("cleaned test image %s\n", imageId);
            } catch (Throwable t) {
                System.out.printf("Failed to clean test image %s: %s\n", imageId, t.toString());
            }
        }
    }

    private String dockerRun(String imageId) {
        String containerId = dockerClient.createContainerCmd(imageId).exec().getId();
        dockerClient.startContainerCmd(containerId).exec();
        return containerId;
    }

    private static void dockerRm(String containerId) {
        try {
            dockerClient.killContainerCmd(containerId).exec();
        } catch (Throwable t) {
            // pass if the container is not running, no need to kill it
        }
        try {
            dockerClient.removeContainerCmd(containerId).exec();
            System.out.printf("cleaned test container %s\n", containerId);
        } catch (Throwable t) {
            System.out.printf("Failed to clean test container %s: %s\n", containerId, t.toString());
        }
    }

    private void verifyDockerHealthCheckResult(String path, boolean status) {
        verifyAwait(path, status, null);
    }

    private void verifyAwait(String imageId, boolean status, String[] command) {
        verifyAwait(imageId, status, command, "500ms");
    }

    private void verifyAwait(String imageId, boolean status, String[] command, String timeout) {
        String containerId = dockerRun(imageId);
        containerIds.add(containerId);
        when(cube.getId()).thenReturn(containerId);
        when(dockerClientExecutor.getDockerClient()).thenReturn(dockerClient);

        Await params = new Await();
        params.setIterations(2);
        params.setSleepPollingTime(timeout);
        params.setCommand(command);

        DockerHealthAwaitStrategy dockerHealthAwaitStrategy = new DockerHealthAwaitStrategy(
            cube, dockerClientExecutor, params
        );
        Assert.assertEquals(status, dockerHealthAwaitStrategy.await());
    }
}
