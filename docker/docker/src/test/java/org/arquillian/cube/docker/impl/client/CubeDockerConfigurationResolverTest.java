package org.arquillian.cube.docker.impl.client;

import javax.ws.rs.ProcessingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InfoCmd;
import com.github.dockerjava.api.model.Info;
import org.arquillian.cube.docker.impl.util.Boot2Docker;
import org.arquillian.cube.docker.impl.util.CommandLineExecutor;
import org.arquillian.cube.docker.impl.util.DefaultDocker;
import org.arquillian.cube.docker.impl.util.DockerMachine;
import org.arquillian.cube.docker.impl.util.OperatingSystem;
import org.arquillian.cube.docker.impl.util.OperatingSystemFamily;
import org.arquillian.cube.docker.impl.util.OperatingSystemFamilyInterface;
import org.arquillian.cube.docker.impl.util.OperatingSystemInterface;
import org.arquillian.cube.docker.impl.util.OperatingSystemResolver;
import org.arquillian.cube.docker.impl.util.Top;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CubeDockerConfigurationResolverTest {

    @ClassRule
    public static final EnvironmentVariables environmentVariables = new EnvironmentVariables();
    
    @Mock
    private static OperatingSystemInterface operatingSystemInterface;

    @Mock
    private static OperatingSystemFamilyInterface defaultOperatingSystemFamilyInterface;

    @Mock
    private static CommandLineExecutor boot2dockerCommandLineExecutor;

    @Mock
    private static DefaultDocker defaultDocker;

    @Mock
    private static DockerClient dockerClient;

    @Mock
    private static InfoCmd infoCmd;

    @Mock
    private static Info info;

    public DefaultDocker mockDefaultDocker() {
        when(defaultDocker.getDefaultDockerClient(anyString())).thenReturn(dockerClient);
        when(dockerClient.infoCmd()).thenReturn(infoCmd);
        when(infoCmd.exec()).thenReturn(info);
        return defaultDocker;
    }
    
    @BeforeClass
    public static void beforeEach() {
        environmentVariables.clear("DOCKER_HOST", "DOCKER_MACHINE_NAME", "DOCKER_TLS_VERIFY", "DOCKER_CERT_PATH");
    }

    @Test
    public void shouldDetectsValidDockerDefault() throws Exception {


        CubeDockerConfigurationResolver resolver = new CubeDockerConfigurationResolver(new Top(),
            new DockerMachine(null),
            new Boot2Docker(null),
            mockDefaultDocker(),
            operatingSystemInterface);
        when(info.getName()).thenReturn("docker-ce");
        when(info.getServerVersion()).thenReturn("arquillian-test");
        when(info.getKernelVersion()).thenReturn("0.0.0");
        final URL sockURL = this.getClass().getClassLoader().getResource("docker.sock");

        boolean isWindows = new OperatingSystemResolver().currentOperatingSystem().getFamily() == OperatingSystemFamily.WINDOWS;

        //file:/C:/Users/kt26ph/GitHub/arquillian/arquillian-cube-pxc/docker/docker/target/test-classes/docker.sock


        String sockUri = isWindows ?sockURL.toExternalForm().replace("file:/", "") : "unix://" + sockURL.toExternalForm();
        when(defaultOperatingSystemFamilyInterface.getServerUri()).thenReturn(sockUri);
        when(operatingSystemInterface.getDefaultFamily()).thenReturn(defaultOperatingSystemFamilyInterface);
        when(operatingSystemInterface.getFamily()).thenReturn(OperatingSystem.MAC_OSX.getFamily());

        Map<String, String> config = new HashMap<>();

        Map<String, String> resolvedConfig = resolver.resolve(config);

        if(!isWindows){
            assertThat(Boolean.valueOf(resolvedConfig.get(CubeDockerConfiguration.TLS_VERIFY)), is(false));
            assertThat(resolvedConfig.get(CubeDockerConfiguration.CERT_PATH), is(nullValue()));
        }

        assertThat(resolvedConfig.get(CubeDockerConfiguration.DOCKER_URI), is(sockUri));
    }

    @Test
    public void shouldSkipsInvalidDockerDefault() throws Exception {

        CubeDockerConfigurationResolver resolver = new CubeDockerConfigurationResolver(new Top(),
            new DockerMachine(null),
            new Boot2Docker(boot2dockerCommandLineExecutor),
            mockDefaultDocker(),
            operatingSystemInterface);
        when(boot2dockerCommandLineExecutor.execCommand(Matchers.<String>anyVararg())).thenReturn("127.0.0.1");

        String sockUri = "unix:///a/path-that/does/not/exist";
        when(defaultOperatingSystemFamilyInterface.getServerUri()).thenReturn(sockUri);
        when(operatingSystemInterface.getDefaultFamily()).thenReturn(defaultOperatingSystemFamilyInterface);
        when(operatingSystemInterface.getFamily()).thenReturn(OperatingSystem.MAC_OSX.getFamily());

        Map<String, String> config = new HashMap<>();

        Map<String, String> resolvedConfig = resolver.resolve(config);

        assertThat(Boolean.valueOf(resolvedConfig.get(CubeDockerConfiguration.TLS_VERIFY)), is(true));
        assertThat(resolvedConfig.get(CubeDockerConfiguration.DOCKER_URI), is("tcp://127.0.0.1:2376"));
    }

    @Test
    public void shouldNotSetTlsVerifyForTcpSchemeOnOSX() {
        CubeDockerConfigurationResolver resolver = new CubeDockerConfigurationResolver(new Top(),
            new DockerMachine(null),
            new Boot2Docker(null),
            mockDefaultDocker(),
            operatingSystemInterface);

        when(infoCmd.exec()).thenThrow(new ProcessingException("test exception"));
        String sockUri = "unix:///a/path-that/does/not/exist";
        when(defaultOperatingSystemFamilyInterface.getServerUri()).thenReturn(sockUri);
        when(operatingSystemInterface.getDefaultFamily()).thenReturn(defaultOperatingSystemFamilyInterface);
        when(operatingSystemInterface.getFamily()).thenReturn(OperatingSystem.MAC_OSX.getFamily());

        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "tcp://localhost:2376");

        Map<String, String> resolvedConfig = resolver.resolve(config);

        assertThat(Boolean.valueOf(resolvedConfig.get(CubeDockerConfiguration.TLS_VERIFY)), is(false));
        assertThat(resolvedConfig.get(CubeDockerConfiguration.CERT_PATH), is(nullValue()));
    }

    @Test
    public void shouldNotSetTlsVerifyForTcpSchemeOnLinux() {
        CubeDockerConfigurationResolver resolver = new CubeDockerConfigurationResolver(new Top(),
            new DockerMachine(null),
            new Boot2Docker(null),
            mockDefaultDocker(),
            operatingSystemInterface);
        when(infoCmd.exec()).thenThrow(new ProcessingException("test exception"));

        String sockUri = "unix:///a/path-that/does/not/exist";
        when(defaultOperatingSystemFamilyInterface.getServerUri()).thenReturn(sockUri);
        when(operatingSystemInterface.getDefaultFamily()).thenReturn(defaultOperatingSystemFamilyInterface);
        when(operatingSystemInterface.getFamily()).thenReturn(OperatingSystem.LINUX_OS.getFamily());

        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "tcp://localhost:2376");

        Map<String, String> resolvedConfig = resolver.resolve(config);

        assertThat(Boolean.valueOf(resolvedConfig.get(CubeDockerConfiguration.TLS_VERIFY)), is(false));
        assertThat(resolvedConfig.get(CubeDockerConfiguration.CERT_PATH), is(nullValue()));
    }
}
