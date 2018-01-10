package org.arquillian.cube.docker.impl.client;

import java.util.HashMap;
import java.util.Map;
import org.arquillian.cube.docker.impl.util.Boot2Docker;
import org.arquillian.cube.docker.impl.util.DockerMachine;
import org.arquillian.cube.docker.impl.util.OperatingSystemFamily;
import org.arquillian.cube.docker.impl.util.Top;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class CubeDockerConfigurationResolverTest {

    @Test
    public void shouldNotSetTlsVerifyForTcpSchemeOnOSX() {
        CubeDockerConfigurationResolver resolver = new CubeDockerConfigurationResolver(new Top(),
            new DockerMachine(null),
            new Boot2Docker(null),
            OperatingSystemFamily.MAC);

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
            OperatingSystemFamily.LINUX);

        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "tcp://localhost:2376");

        Map<String, String> resolvedConfig = resolver.resolve(config);

        assertThat(Boolean.valueOf(resolvedConfig.get(CubeDockerConfiguration.TLS_VERIFY)), is(false));
        assertThat(resolvedConfig.get(CubeDockerConfiguration.CERT_PATH), is(nullValue()));
    }
}
