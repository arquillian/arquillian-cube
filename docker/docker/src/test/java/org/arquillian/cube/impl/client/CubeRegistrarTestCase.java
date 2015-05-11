package org.arquillian.cube.impl.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.arquillian.cube.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.model.DockerCubeRegistry;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CubeRegistrarTestCase extends AbstractManagerTestBase {

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(CubeRegistrar.class);
        super.addExtensions(extensions);
    }

    private static final String CONTENT = "tomcat:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  exposedPorts: [8089/tcp]\n" +
            "  await:\n" +
            "    strategy: static\n" +
            "    ip: localhost\n" +
            "    ports: [8080, 8089]";


    @Mock
    private DockerClientExecutor executor;

    @Inject
    private Instance<CubeRegistry> registryInst;

    @Test
    public void shouldExposeAndRegisterCubesFromConfiguration() {
        bind(ApplicationScoped.class, CubeConfiguration.class, createConfig());
        fire(executor);

        assertEventFired(DockerCubeRegistry.class, 1); // Events are recorded by exact type

        CubeRegistry registry = registryInst.get();
        Assert.assertEquals(1, registry.getCubes().size());
        Assert.assertEquals("tomcat", registry.getCubes().get(0).getId());
    }

    private CubeConfiguration createConfig() {
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("serverVersion", "1.13");
        parameters.put("serverUri", "http://localhost:25123");
        parameters.put("dockerContainers", CONTENT);

        return CubeConfiguration.fromMap(parameters);
    }
}
