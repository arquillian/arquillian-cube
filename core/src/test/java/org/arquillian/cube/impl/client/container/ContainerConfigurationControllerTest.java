package org.arquillian.cube.impl.client.container;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.arquillian.cube.impl.model.LocalCubeRegistry;
import org.arquillian.cube.impl.util.TestPortBindings;
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

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ContainerConfigurationControllerTest extends AbstractManagerTestBase {

    public static final String CUBE_ID = "test";
    private static final String CONTENT = "" + "image: tomcat:10.1.30\n" + "exposedPorts: [8089/tcp]\n"
        + "portBindings: [8090->8089/tcp]";

    @Mock
    private Cube<Map<String, Object>> cube;

    @Mock
    private Container container;

    @SuppressWarnings("rawtypes")
    @Mock
    private DeployableContainer deployableContainer;

    @Mock
    private ContainerRegistry containerRegistry;

    @Mock
    private ContainerDef containerDef;

    private CubeRegistry registry;

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(ContainerConfigurationController.class);
        super.addExtensions(extensions);
    }

    @Before
    public void setup() {

        Yaml yaml = new Yaml();
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) yaml.load(CONTENT);

        when(cube.getId()).thenReturn(CUBE_ID);
        lenient().when(cube.configuration()).thenReturn(content);
        when(cube.getMetadata(HasPortBindings.class)).thenReturn(
            new TestPortBindings(new Binding("localhost").addPortBinding(8089, 8090)));
        when(container.getName()).thenReturn(CUBE_ID);
        when(container.getDeployableContainer()).thenReturn(deployableContainer);
        when(deployableContainer.getConfigurationClass()).thenReturn(ContainerConfiguration.class);
        when(container.getContainerConfiguration()).thenReturn(containerDef);
        when(containerRegistry.getContainers()).thenReturn(Arrays.asList(container));
        registry = new LocalCubeRegistry();
        registry.addCube(cube);

        bind(ApplicationScoped.class, CubeRegistry.class, registry);
        bind(ApplicationScoped.class, ContainerRegistry.class, containerRegistry);
    }

    @Test
    public void shouldRemapContainerPortIfItIsEqualToExposedOne() {

        Map<String, String> containerConfig = new HashMap<String, String>();
        when(containerDef.getContainerProperties()).thenReturn(containerConfig);

        fire(new BeforeSetup(deployableContainer));
        verify(containerDef).overrideProperty("port", "8090");
    }

    @Test
    public void shouldNotRemapContainerPortIfItIsNotEqualToExposedOne() {

        Map<String, String> containerConfig = new HashMap<String, String>();
        containerConfig.put("port", "8090");
        when(containerDef.getContainerProperties()).thenReturn(containerConfig);

        fire(new BeforeSetup(deployableContainer));
        verify(containerDef, times(0)).overrideProperty("port", "8090");
        verify(containerDef, times(0)).overrideProperty("port", "8089");
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
}
