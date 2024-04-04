package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.AfterDestroy;
import org.arquillian.cube.spi.event.lifecycle.AfterStart;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SystemPropertiesCubeSetterTest {

    @Mock
    CubeRegistry cubeRegistry;

    @Mock
    DockerCube dockerCube;

    @Mock
    Binding binding;

    @Mock
    Binding.PortBinding portBinding;

    @Mock
    DockerClientExecutor dockerClientExecutor;

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Before
    public void setupContainers() {

        when(dockerClientExecutor.getDockerServerIp()).thenReturn("192.168.99.100");

        when(cubeRegistry.getCube("image", DockerCube.class)).thenReturn(dockerCube);
        when(dockerCube.bindings()).thenReturn(binding);
        when(binding.getIP()).thenReturn("192.168.99.100");
        when(binding.getInternalIP()).thenReturn("172.0.10.0");
        when(portBinding.getExposedPort()).thenReturn(8080);
        when(portBinding.getBindingPort()).thenReturn(8080);

        Set<Binding.PortBinding> portBindings = new HashSet<>();
        portBindings.add(portBinding);
        when(binding.getPortBindings()).thenReturn(portBindings);
    }

    @Test
    public void should_set_cube_properties() {

        // given
        SystemPropertiesCubeSetter systemPropertiesCubeSetter = new SystemPropertiesCubeSetter();

        // when
        systemPropertiesCubeSetter.createCubeSystemProperties(new AfterStart("image"), cubeRegistry);

        // then
        assertThat(System.getProperty("arq.cube.docker.image.ip")).isEqualTo("192.168.99.100");
        assertThat(System.getProperty("arq.cube.docker.image.internal.ip")).isEqualTo("172.0.10.0");
        assertThat(System.getProperty("arq.cube.docker.image.port.8080")).isEqualTo("8080");

    }

    @Test
    public void should_unset_cube_properties() {

        //given

        SystemPropertiesCubeSetter systemPropertiesCubeSetter = new SystemPropertiesCubeSetter();

        System.setProperty("arq.cube.docker.image.ip", "192.168.99.100");
        System.setProperty("arq.cube.docker.image.port.8080", "8080");

        // when
        systemPropertiesCubeSetter.removeCubeSystemProperties(new AfterDestroy("image"));

        // then
        assertThat(System.getProperty("arq.cube.docker.image.ip")).isNull();
        assertThat(System.getProperty("arq.cube.docker.image.port.8080")).isNull();

    }

    @Test
    public void should_set_docker_host_ip() {

        // given
        SystemPropertiesCubeSetter systemPropertiesCubeSetter = new SystemPropertiesCubeSetter();

        // when
        systemPropertiesCubeSetter.createDockerHostProperty(new BeforeSuite(), dockerClientExecutor);

        // then
        assertThat(System.getProperty("arq.cube.docker.host")).isEqualTo("192.168.99.100");

    }

    @Test
    public void should_unset_docker_host_ip() {

        // given
        SystemPropertiesCubeSetter systemPropertiesCubeSetter = new SystemPropertiesCubeSetter();

        System.setProperty("arq.cube.docker.host", "192.168.99.100");


        // when
        systemPropertiesCubeSetter.removeDockerHostProperty(new AfterSuite());

        // then
        assertThat(System.getProperty("arq.cube.docker.host")).isNull();

    }

}
