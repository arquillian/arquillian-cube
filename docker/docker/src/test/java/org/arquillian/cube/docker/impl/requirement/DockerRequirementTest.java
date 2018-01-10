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

import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.client.CubeDockerConfigurationResolver;
import org.arquillian.cube.docker.impl.util.CommandLineExecutor;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;
import org.arquillian.spacelift.execution.ExecutionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DockerRequirementTest {

    @Mock
    CubeDockerConfigurationResolver configResolver;

    @Mock
    CommandLineExecutor commandLineExecutor;

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
            requirement.check(null);
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
            return "tcp://" + serverSocket.getInetAddress().getHostName() + ":" + serverSocket.getLocalPort();
        }

        @Override
        public void run() {
            PrintWriter writer = null;
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

                String versionJSON = "{\"Client\":{\"Version\":\"0.0.0\",\"ApiVersion\":\"0.00\"}}";
                writer.println("HTTP/1.1 200 OK");
                writer.println("Content-Type: application/json");
                writer.println("Content-Length: " + versionJSON.length());
                writer.println();
                writer.println(versionJSON);
            } catch (IOException e) {
                writer.println("HTTP/1.1 500");
                writer.println();
            } finally {
                if (writer != null) {
                    writer.close();
                }

                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        return;
                    }
                }
            }
            return;
        }
    }
}
