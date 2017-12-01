package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.util.Boot2Docker;
import org.arquillian.cube.docker.impl.util.CommandLineExecutor;
import org.arquillian.cube.docker.impl.util.DockerMachine;
import org.arquillian.cube.docker.impl.util.OperatingSystem;
import org.arquillian.cube.docker.impl.util.OperatingSystemFamilyInterface;
import org.arquillian.cube.docker.impl.util.OperatingSystemInterface;
import org.arquillian.cube.docker.impl.util.Top;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CubeDockerConfigurationResolverTest {

    @Mock
    private static OperatingSystemInterface operatingSystemInterface;

    @Mock
    private static OperatingSystemFamilyInterface defaultOperatingSystemFamilyInterface;

    @Mock
    private static CommandLineExecutor boot2dockerCommandLineExecutor;

    @Test
    public void shouldDetectsValidDockerDefault() throws Exception {

        CubeDockerConfigurationResolver resolver = new CubeDockerConfigurationResolver(new Top(),
            new DockerMachine(null),
            new Boot2Docker(null),
            operatingSystemInterface);
        URL sockURL = this.getClass().getClassLoader().getResource("docker.sock");

        String sockUri = "unix://" + sockURL.getPath();
        when(defaultOperatingSystemFamilyInterface.getServerUri()).thenReturn(sockUri);
        when(operatingSystemInterface.getDefaultFamily()).thenReturn(defaultOperatingSystemFamilyInterface);
        when(operatingSystemInterface.getFamily()).thenReturn(OperatingSystem.MAC_OSX.getFamily());

        Map<String, String> config = new HashMap<>();

        Map<String, String> resolvedConfig = resolver.resolve(config);

        assertThat(Boolean.valueOf(resolvedConfig.get(CubeDockerConfiguration.TLS_VERIFY)), is(false));
        assertThat(resolvedConfig.get(CubeDockerConfiguration.CERT_PATH), is(nullValue()));
        assertThat(resolvedConfig.get(CubeDockerConfiguration.DOCKER_URI), is(sockUri));
    }

    @Test
    public void shouldSkipsInvalidDockerDefault() throws Exception {

        CubeDockerConfigurationResolver resolver = new CubeDockerConfigurationResolver(new Top(),
            new DockerMachine(null),
            new Boot2Docker(boot2dockerCommandLineExecutor),
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
            operatingSystemInterface);

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
            operatingSystemInterface);

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
