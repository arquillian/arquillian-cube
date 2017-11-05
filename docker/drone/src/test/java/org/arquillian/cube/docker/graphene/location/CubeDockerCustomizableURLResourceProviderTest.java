package org.arquillian.cube.docker.graphene.location;

import java.net.URL;
import org.arquillian.cube.docker.drone.SeleniumContainers;
import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.util.ConfigUtil;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.graphene.spi.configuration.GrapheneConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private CubeRegistry cubeRegistry;

    @Mock
    private Cube cube;

    @Mock
    private HasPortBindings hasPortBindings;

    private DockerCubeCustomizableURLResourceProvider dockerCubeCustomizableURLResourceProvider;

    @Before
    public void prepareCubeDockerConfiguration() {
        when(cubeDockerConfiguration.getDockerServerIp()).thenReturn(DOCKER_HOST);

        when(hasPortBindings.getInternalIP()).thenReturn("192.168.99.100");
        when(cube.hasMetadata(HasPortBindings.class)).thenReturn(true);
        when(cube.getMetadata(HasPortBindings.class)).thenReturn(hasPortBindings);
        when(cubeRegistry.getCube("helloworld")).thenReturn(cube);

        dockerCubeCustomizableURLResourceProvider = new DockerCubeCustomizableURLResourceProvider();
        dockerCubeCustomizableURLResourceProvider.cubeDockerConfigurationInstance =
            new Instance<CubeDockerConfiguration>() {
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

        assertThatThrownBy(() -> {
            dockerCubeCustomizableURLResourceProvider.lookup(null);
        })
            .hasMessage("Arquillian Cube Graphene integration should provide a URL in Graphene extension configuration.");
    }

    @Test
    public void should_resolve_internal_ip_of_container() {
        final DockerCompositions compositions = ConfigUtil.load(SIMPLE_SCENARIO);
        when(cubeDockerConfiguration.getDockerContainersContent()).thenReturn(compositions);
        when(grapheneConfiguration.getUrl()).thenReturn("http://helloworld:80/context");

        final URL url = (URL) dockerCubeCustomizableURLResourceProvider.lookup(null);

        assertThat(url)
            .hasProtocol("http")
            .hasHost("192.168.99.100")
            .hasPort(80)
            .hasPath("/context");
    }

    @Test
    public void should_resolve_internal_ip_of_container_with_default_port() {
        final DockerCompositions compositions = ConfigUtil.load(SIMPLE_SCENARIO);
        when(cubeDockerConfiguration.getDockerContainersContent()).thenReturn(compositions);
        when(grapheneConfiguration.getUrl()).thenReturn("http://helloworld/context");

        final URL url = (URL) dockerCubeCustomizableURLResourceProvider.lookup(null);

        assertThat(url)
            .hasProtocol("http")
            .hasHost("192.168.99.100")
            .hasPort(80)
            .hasPath("/context");
    }

    @Test
    public void should_not_resolve_ip() {
        final DockerCompositions compositions = ConfigUtil.load(SIMPLE_SCENARIO);
        when(cubeDockerConfiguration.getDockerContainersContent()).thenReturn(compositions);
        when(grapheneConfiguration.getUrl()).thenReturn("http://192.168.99.101:80/context");

        final URL url = (URL) dockerCubeCustomizableURLResourceProvider.lookup(null);

        assertThat(url)
            .hasProtocol("http")
            .hasHost("192.168.99.101")
            .hasPort(80)
            .hasPath("/context");
    }

    @Test
    public void should_resolve_docker_host_in_relative_url() {
        final DockerCompositions compositions = ConfigUtil.load(SIMPLE_SCENARIO);
        when(cubeDockerConfiguration.getDockerContainersContent()).thenReturn(compositions);
        when(grapheneConfiguration.getUrl()).thenReturn("http://dockerHost:80/context");

        final URL url = (URL) dockerCubeCustomizableURLResourceProvider.lookup(null);

        assertThat(url)
            .hasProtocol("http")
            .hasHost("192.168.99.100")
            .hasPort(80)
            .hasPath("/context");
    }

    @Test
    public void should_resolve_docker_host_in_relative_url_with_default_port() {
        final DockerCompositions compositions = ConfigUtil.load(SIMPLE_SCENARIO);
        when(cubeDockerConfiguration.getDockerContainersContent()).thenReturn(compositions);
        when(grapheneConfiguration.getUrl()).thenReturn("http://dockerHost/context");

        final URL url = (URL) dockerCubeCustomizableURLResourceProvider.lookup(null);

        assertThat(url)
            .hasProtocol("http")
            .hasHost("192.168.99.100")
            .hasPort(80)
            .hasPath("/context");
    }
}
