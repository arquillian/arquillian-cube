package org.arquillian.cube.impl.client.container;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.arquillian.cube.impl.model.DockerCubeRegistry;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
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
import org.mockito.runners.MockitoJUnitRunner;
import org.yaml.snakeyaml.Yaml;

@RunWith(MockitoJUnitRunner.class)
public class RemapContainerControllerTest extends AbstractManagerTestBase {

    public static final String CUBE_ID = "test";
    private static final String CONTENT = "" + "image: tutum/tomcat:7.0\n" + "exposedPorts: [8089/tcp]\n"
            + "portBindings: \n" + "  - exposedPort: 8089/tcp\n" + "    port: 8090";

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

    private CubeRegistry registry;

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(RemapContainerController.class);
        super.addExtensions(extensions);
    }

    public static class ContainerConfiguration {
        private int port = 8089;
        
        public int getPort() {
            return port;
        }
        
        public void setPort(int port) {
            this.port = port;
        }
    }
    
    @Before
    public void setup() {

        Yaml yaml = new Yaml();
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) yaml.load(CONTENT);

        when(cube.getId()).thenReturn(CUBE_ID);
        when(cube.configuration()).thenReturn(content);
        when(container.getName()).thenReturn(CUBE_ID);
        when(container.getDeployableContainer()).thenReturn(deployableContainer);
        when(deployableContainer.getConfigurationClass()).thenReturn(ContainerConfiguration.class);
        when(container.getContainerConfiguration()).thenReturn(containerDef);
        when(containerRegistry.getContainers()).thenReturn(Arrays.asList(container));
        registry = new DockerCubeRegistry();
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

}
