package org.arquillian.cube.docker.impl.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.model.LocalCubeRegistry;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CubeRegistrarTestCase extends AbstractManagerTestBase {

    private static final String CONTENT = "tomcat:\n" +
        "  image: tutum/tomcat:7.0\n" +
        "  exposedPorts: [8089/tcp]\n" +
        "  await:\n" +
        "    strategy: static\n" +
        "    ip: localhost\n" +
        "    ports: [8080, 8089]";
    @Mock
    private DockerClientExecutor executor;
    private CubeRegistry registry = new LocalCubeRegistry();

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(CubeDockerRegistrar.class);
        super.addExtensions(extensions);
    }

    @Before
    public void setup() {
        bind(ApplicationScoped.class, CubeRegistry.class, registry);
        when(executor.isDockerInsideDockerResolution()).thenReturn(true);
    }

    @Test
    public void shouldExposeAndRegisterCubesFromConfiguration() {
        bind(ApplicationScoped.class, CubeDockerConfiguration.class, createConfig());
        fire(executor);

        Assert.assertEquals(1, registry.getCubes().size());
        Assert.assertEquals("tomcat", registry.getCubes().get(0).getId());
    }

    private CubeDockerConfiguration createConfig() {
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());
        parameters.put("dockerContainers", CONTENT);

        return CubeDockerConfiguration.fromMap(parameters, null);
    }
}
