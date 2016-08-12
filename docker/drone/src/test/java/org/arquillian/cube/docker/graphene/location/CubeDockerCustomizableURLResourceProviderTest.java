package org.arquillian.cube.docker.graphene.location;

import org.arquillian.cube.docker.drone.SeleniumContainers;
import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.util.ConfigUtil;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.CubeMetadata;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.graphene.spi.configuration.GrapheneConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CubeDockerCustomizableURLResourceProviderTest {

    private static final String DOCKER_HOST = "192.168.99.100";

    private static final String SIMPLE_SCENARIO =
            "helloworld:\n" +
            "  image: dockercloud/hello-world\n" +
            "  portBindings: [8080->80/tcp]";

    private static final String MULTIPLE_PORT_BINDING_SCENARIO =
            "helloworld:\n" +
            "  image: dockercloud/hello-world\n" +
            "  portBindings: [8080->80/tcp, 8081->81/tcp]";

    private static final String MULTIPLE_CONTAINER_SCENARIO =
            "helloworld:\n" +
            "  image: dockercloud/hello-world\n" +
            "  portBindings: [8080->80/tcp]\n" +
            "helloworld2:\n" +
            "  image: dockercloud/hello-world";

    @Mock
    private GrapheneConfiguration grapheneConfiguration;

    @Mock
    private CubeDockerConfiguration cubeDockerConfiguration;

    @Mock
    private SeleniumContainers seleniumContainers;

    @Mock
    private CubeRegistry cubeRegistry;

    @Mock
    private Cube cube;

    @Mock
    private HasPortBindings hasPortBindings;

    private DockerCubeCustomizableURLResourceProvider dockerCubeCustomizableURLResourceProvider;

    @Before
    public void prepareCubeDockerConfiguration() {
        when(cubeDockerConfiguration.getDockerServerIp()).thenReturn(DOCKER_HOST);
        when(seleniumContainers.getSeleniumContainerName()).thenReturn(SeleniumContainers.SELENIUM_CONTAINER_NAME);
        when(seleniumContainers.getVncContainerName()).thenReturn(SeleniumContainers.VNC_CONTAINER_NAME);

        when(hasPortBindings.getInternalIP()).thenReturn("192.168.99.100");
        when(cube.hasMetadata(HasPortBindings.class)).thenReturn(true);
        when(cube.getMetadata(HasPortBindings.class)).thenReturn(hasPortBindings);
        when(cubeRegistry.getCube("helloworld")).thenReturn(cube);


        dockerCubeCustomizableURLResourceProvider = new DockerCubeCustomizableURLResourceProvider();
        dockerCubeCustomizableURLResourceProvider.cubeDockerConfigurationInstance = new Instance<CubeDockerConfiguration>() {
            @Override
            public CubeDockerConfiguration get() {
                return cubeDockerConfiguration;
            }
        };
        dockerCubeCustomizableURLResourceProvider.grapheneConfiguration = new Instance<GrapheneConfiguration>() {
            @Override
            public GrapheneConfiguration get() {
                return grapheneConfiguration;
            }
        };
        dockerCubeCustomizableURLResourceProvider.seleniumContainersInstance = new Instance<SeleniumContainers>() {
            @Override
            public SeleniumContainers get() {
                return seleniumContainers;
            }
        };
        dockerCubeCustomizableURLResourceProvider.cubeRegistryInstance = new Instance<CubeRegistry>() {
            @Override
            public CubeRegistry get() {
                return cubeRegistry;
            }
        };
    }

    @Test
    public void should_resolve_to_docker_host_if_no_url_provided() {
        final DockerCompositions compositions = ConfigUtil.load(SIMPLE_SCENARIO);
        when(cubeDockerConfiguration.getDockerContainersContent()).thenReturn(compositions);
        when(grapheneConfiguration.getUrl()).thenReturn(null);

        final URL url = (URL) dockerCubeCustomizableURLResourceProvider.lookup(null);

        assertThat(url)
                .hasProtocol("http")
                .hasHost("192.168.99.100")
                .hasPort(8080)
                .hasNoPath();
    }

    @Test
    public void should_resolve_full_absolute_url() {
        final DockerCompositions compositions = ConfigUtil.load(SIMPLE_SCENARIO);
        when(cubeDockerConfiguration.getDockerContainersContent()).thenReturn(compositions);
        when(grapheneConfiguration.getUrl()).thenReturn("/192.168.99.100:80/context");

        final URL url = (URL) dockerCubeCustomizableURLResourceProvider.lookup(null);

        assertThat(url)
                .hasProtocol("http")
                .hasHost("192.168.99.100")
                .hasPort(8080)
                .hasPath("context");

    }

    @Test
    public void should_resolve_port_full_absolute_url() {
        final DockerCompositions compositions = ConfigUtil.load(SIMPLE_SCENARIO);
        when(cubeDockerConfiguration.getDockerContainersContent()).thenReturn(compositions);
        when(grapheneConfiguration.getUrl()).thenReturn("/192.168.99.100/context");

        final URL url = (URL) dockerCubeCustomizableURLResourceProvider.lookup(null);

        assertThat(url)
                .hasProtocol("http")
                .hasHost("192.168.99.100")
                .hasPort(8080)
                .hasPath("context");
    }

    @Test
    public void should_resolve_return_as_exposed_port_in_case_of_specfied_port_without_binding_full_absolute_url() {
        final DockerCompositions compositions = ConfigUtil.load(SIMPLE_SCENARIO);
        when(cubeDockerConfiguration.getDockerContainersContent()).thenReturn(compositions);
        when(grapheneConfiguration.getUrl()).thenReturn("/192.168.99.100:8081/context");

        final URL url = (URL) dockerCubeCustomizableURLResourceProvider.lookup(null);

        assertThat(url)
                .hasProtocol("http")
                .hasHost("192.168.99.100")
                .hasPort(8081)
                .hasPath("context");
    }

    @Test
    public void should_resolve_docker_host_in_absolute_url() {
        final DockerCompositions compositions = ConfigUtil.load(SIMPLE_SCENARIO);
        when(cubeDockerConfiguration.getDockerContainersContent()).thenReturn(compositions);
        when(grapheneConfiguration.getUrl()).thenReturn("/dockerHost:80/context");

        final URL url = (URL) dockerCubeCustomizableURLResourceProvider.lookup(null);

        assertThat(url)
                .hasProtocol("http")
                .hasHost("192.168.99.100")
                .hasPort(8080)
                .hasPath("context");
    }

    @Test
    public void should_resolve_docker_host_in_relative_url() {
        final DockerCompositions compositions = ConfigUtil.load(SIMPLE_SCENARIO);
        when(cubeDockerConfiguration.getDockerContainersContent()).thenReturn(compositions);
        when(grapheneConfiguration.getUrl()).thenReturn("dockerHost:80/context");

        final URL url = (URL) dockerCubeCustomizableURLResourceProvider.lookup(null);

        assertThat(url)
                .hasProtocol("http")
                .hasHost("192.168.99.100")
                .hasPort(8080)
                .hasPath("context");
    }

    @Test
    public void should_resolve_docker_host_and_port_in_relative_url() {
        final DockerCompositions compositions = ConfigUtil.load(SIMPLE_SCENARIO);
        when(cubeDockerConfiguration.getDockerContainersContent()).thenReturn(compositions);
        when(grapheneConfiguration.getUrl()).thenReturn("context");

        final URL url = (URL) dockerCubeCustomizableURLResourceProvider.lookup(null);

        assertThat(url)
                .hasProtocol("http")
                .hasHost("192.168.99.100")
                .hasPort(8080)
                .hasPath("context");
    }

    @Test
    public void should_resolve_full_absolute_url_with_specified_port_and_multiple_port_bindings() {
        final DockerCompositions compositions = ConfigUtil.load(MULTIPLE_PORT_BINDING_SCENARIO);
        when(cubeDockerConfiguration.getDockerContainersContent()).thenReturn(compositions);
        when(grapheneConfiguration.getUrl()).thenReturn("/192.168.99.100:80/context");

        final URL url = (URL) dockerCubeCustomizableURLResourceProvider.lookup(null);

        assertThat(url)
                .hasProtocol("http")
                .hasHost("192.168.99.100")
                .hasPort(8080)
                .hasPath("context");

    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_full_absolute_url_and_multiple_port_binding() {
        final DockerCompositions compositions = ConfigUtil.load(MULTIPLE_PORT_BINDING_SCENARIO);
        when(cubeDockerConfiguration.getDockerContainersContent()).thenReturn(compositions);
        when(grapheneConfiguration.getUrl()).thenReturn("/192.168.99.100/context");

        final URL url = (URL) dockerCubeCustomizableURLResourceProvider.lookup(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_relative_url_and_multiple_port_binding() {
        final DockerCompositions compositions = ConfigUtil.load(MULTIPLE_PORT_BINDING_SCENARIO);
        when(cubeDockerConfiguration.getDockerContainersContent()).thenReturn(compositions);
        when(grapheneConfiguration.getUrl()).thenReturn("/192.168.99.100/context");

        final URL url = (URL) dockerCubeCustomizableURLResourceProvider.lookup(null);
    }

    @Test
    public void should_resolve_port_absolute_url_with_multiple_container() {
        final DockerCompositions compositions = ConfigUtil.load(MULTIPLE_CONTAINER_SCENARIO);
        when(cubeDockerConfiguration.getDockerContainersContent()).thenReturn(compositions);
        when(grapheneConfiguration.getUrl()).thenReturn("/192.168.99.100:80/context");

        final URL url = (URL) dockerCubeCustomizableURLResourceProvider.lookup(null);

        assertThat(url)
                .hasProtocol("http")
                .hasHost("192.168.99.100")
                .hasPort(8080)
                .hasPath("context");
    }

    @Test
    public void should_resolve_not_specified_port_absolute_url_with_multiple_container() {
        final DockerCompositions compositions = ConfigUtil.load(MULTIPLE_CONTAINER_SCENARIO);
        when(cubeDockerConfiguration.getDockerContainersContent()).thenReturn(compositions);
        when(grapheneConfiguration.getUrl()).thenReturn("/192.168.99.100/context");

        final URL url = (URL) dockerCubeCustomizableURLResourceProvider.lookup(null);

        assertThat(url)
                .hasProtocol("http")
                .hasHost("192.168.99.100")
                .hasPort(8080)
                .hasPath("context");
    }

}
