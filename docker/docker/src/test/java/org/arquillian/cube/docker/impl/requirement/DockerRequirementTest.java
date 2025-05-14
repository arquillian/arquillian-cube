package org.arquillian.cube.docker.impl.requirement;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.client.CubeDockerConfigurationResolver;
import org.arquillian.cube.docker.impl.util.CommandLineExecutor;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;
import org.arquillian.spacelift.execution.ExecutionException;
import org.awaitility.Awaitility;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DockerRequirementTest {

    @ClassRule
    public static final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Mock
    CubeDockerConfigurationResolver configResolver;

    @Mock
    CommandLineExecutor commandLineExecutor;

    @BeforeClass
    public static void beforeEach() {
        environmentVariables.clear("DOCKER_HOST", "DOCKER_MACHINE_NAME", "DOCKER_TLS_VERIFY", "DOCKER_CERT_PATH");
    }

    @Test(expected = UnsatisfiedRequirementException.class)
    public void testDockerRequirementCheckWhenExecutionExceptionThrown() throws UnsatisfiedRequirementException {
        when(configResolver.resolve(anyMap())).thenThrow(ExecutionException.class);

        DockerRequirement requirement = new DockerRequirement(commandLineExecutor, configResolver);
        requirement.check(null);
    }

    @Test(expected = UnsatisfiedRequirementException.class)
    public void testDockerRequirementCheckWhenDockerURLIsNull() throws UnsatisfiedRequirementException {
        Map<String, String> configMap = new HashMap<>();
        configMap.put(CubeDockerConfiguration.DOCKER_URI, null);

        when(configResolver.resolve(anyMap())).thenReturn(configMap);

        DockerRequirement requirement = new DockerRequirement(commandLineExecutor, configResolver);
        requirement.check(null);
    }

    @Test(expected = UnsatisfiedRequirementException.class)
    public void testDockerRequirementCheckWhenDockerURLIsEmpty() throws UnsatisfiedRequirementException {
        Map<String, String> configMap = new HashMap<>();
        configMap.put(CubeDockerConfiguration.DOCKER_URI, "");

        when(configResolver.resolve(anyMap())).thenReturn(configMap);

        DockerRequirement requirement = new DockerRequirement(commandLineExecutor, configResolver);
        requirement.check(null);
    }

    @Test(expected = UnsatisfiedRequirementException.class)
    public void testDockerRequirementCheckWhenDockerURLIsInvalid() throws UnsatisfiedRequirementException {
        Map<String, String> configMap = new HashMap<>();
        configMap.put(CubeDockerConfiguration.DOCKER_URI, "tcp://nonexistanthostname:9999");

        when(configResolver.resolve(anyMap())).thenReturn(configMap);

        DockerRequirement requirement = new DockerRequirement(commandLineExecutor, configResolver);
        requirement.check(null);
    }

    @Test
    public void testDockerRequirementCheckDockerURLIsValid() throws Exception {
        FakeDockerServer server = new FakeDockerServer();

        Map<String, String> configMap = new HashMap<>();
        configMap.put(CubeDockerConfiguration.DOCKER_URI, server.getConnectionString());

        when(configResolver.resolve(anyMap())).thenReturn(configMap);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(server);

        try {
            DockerRequirement requirement = new DockerRequirement(commandLineExecutor, configResolver);
            Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until( () -> {
                    try {
                        requirement.check(null);
                        return true;
                    } catch(UnsatisfiedRequirementException e) {
                        return false;
                    }
                } );
        } finally {
            executorService.shutdownNow();
        }
    }

    private class FakeDockerServer implements Runnable {

        private final ServerSocket serverSocket;

        private FakeDockerServer() throws IOException {
            serverSocket = new ServerSocket(0);
        }

        public String getConnectionString() {
            // TODO - this doesn't seem to work anymore on GitHub runners, so the CI checks fail, hence we need to
            //  hardcode the loopback IP addrress to be able and move on...
            return "tcp://127.0.0.1"
                // + serverSocket.getInetAddress().getHostName()
                + ":" + serverSocket.getLocalPort();
        }

        @Override
        public void run() {
            PrintWriter writer;
            Socket socket;
            try {
                socket = serverSocket.accept();
                try {
                    socket.getInputStream()
                        .read(); // Will hang on windows if stream is not read
                    writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                    try {
                        String versionJSON = "{\"Client\":{\"Version\":\"0.0.0\",\"ApiVersion\":\"0.00\"}}";
                        writer.println("HTTP/1.1 200 OK");
                        writer.println("Content-Type: application/json");
                        writer.println("Content-Length: " + versionJSON.length());
                        writer.println();
                        writer.println(versionJSON);
                    } finally {
                        writer.close();
                    }
                } finally {
                    socket.close();
                }
            } catch (Exception e) {
                throw new IllegalStateException("Couldn't create socket connection " + e.getMessage());
            }
        }
    }
}
