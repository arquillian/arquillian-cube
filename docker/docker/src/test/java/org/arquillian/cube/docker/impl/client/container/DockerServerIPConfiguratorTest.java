package org.arquillian.cube.docker.impl.client.container;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.arquillian.cube.docker.impl.util.OperatingSystemFamily;
import org.arquillian.cube.impl.model.LocalCubeRegistry;
import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.jboss.arquillian.config.descriptor.api.ContainerDef;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.event.container.BeforeSetup;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.yaml.snakeyaml.Yaml;

@RunWith(MockitoJUnitRunner.class)
public class DockerServerIPConfiguratorTest extends AbstractManagerTestBase {

    public static final String CUBE_ID = "test";
    private static final String CONTENT = "" + "image: tutum/tomcat:7.0\n" + "exposedPorts: [8089/tcp]\n"
            + "portBindings: [8090->8089/tcp]";

    @Mock
    private Cube cube;

    @Mock
    private Container container;

    @SuppressWarnings("rawtypes")
    @Mock
    private DeployableContainer deployableContainer;

    @Mock
    private ContainerRegistry containerRegistry;

    @Mock
    private ContainerDef containerDef;

    @Mock
    private HasPortBindings hasPortBindings;

    private CubeRegistry registry;

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(DockerServerIPConfigurator.class);
        super.addExtensions(extensions);
    }

    public static class ContainerConfiguration {
        private int port = 8089;
        private String myHost = "localhost";
        public int getPort() {
            return port;
        }
        public void setPort(int port) {
            this.port = port;
        }
        public String getMyHost() {
            return myHost;
        }
        public void setMyHost(String myHost) {
            this.myHost = myHost;
        }
    }

    @Before
    public void setup() {

        Yaml yaml = new Yaml();
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) yaml.load(CONTENT);

        when(cube.getId()).thenReturn(CUBE_ID);
        lenient().when(cube.configuration()).thenReturn(content);
        when(container.getName()).thenReturn(CUBE_ID);
        when(container.getDeployableContainer()).thenReturn(deployableContainer);
        when(deployableContainer.getConfigurationClass()).thenReturn(ContainerConfiguration.class);
        when(container.getContainerConfiguration()).thenReturn(containerDef);
        when(containerRegistry.getContainers()).thenReturn(Arrays.asList(container));
        when(hasPortBindings.getContainerIP()).thenReturn("192.168.0.1");
        registry = new LocalCubeRegistry();
        registry.addCube(cube);

        bind(ApplicationScoped.class, CubeRegistry.class, registry);
        bind(ApplicationScoped.class, ContainerRegistry.class, containerRegistry);

    }

    @Test
    public void shouldRemapContainerAddressToBootToDocker() {
        Map<String, String> containerConfig = new HashMap<String, String>();
        when(containerDef.getContainerProperties()).thenReturn(containerConfig);
        when(cube.getMetadata(HasPortBindings.class)).thenReturn(hasPortBindings);
        bind(ApplicationScoped.class, OperatingSystemFamily.class, OperatingSystemFamily.MAC);
        fire(new BeforeSetup(deployableContainer));
        verify(containerDef).overrideProperty("myHost", "192.168.0.1");
    }

    @Test
    public void shouldSubstituteDockerServerIpContainerAddressToBootToDockerIp() {
        Map<String, String> containerConfig = new HashMap<String, String>();
        containerConfig.put("myHost", CubeDockerConfiguration.DOCKER_SERVER_IP);
        when(containerDef.getContainerProperties()).thenReturn(containerConfig);
        when(cube.getMetadata(HasPortBindings.class)).thenReturn(hasPortBindings);
        bind(ApplicationScoped.class, OperatingSystemFamily.class, OperatingSystemFamily.MAC);
        fire(new BeforeSetup(deployableContainer));
        verify(containerDef).overrideProperty("myHost", "192.168.0.1");
    }

    @Test
    public void shouldNotRemapContainerAddressToBootToDocker() {
        Map<String, String> containerConfig = new HashMap<String, String>();
        containerConfig.put("myHost", "10.0.10.1");
        when(containerDef.getContainerProperties()).thenReturn(containerConfig);
        when(cube.getMetadata(HasPortBindings.class)).thenReturn(hasPortBindings);
        bind(ApplicationScoped.class, OperatingSystemFamily.class, OperatingSystemFamily.MAC);
        fire(new BeforeSetup(deployableContainer));
        verify(containerDef, times(0)).overrideProperty("myHost", "192.168.0.1");
    }
}
