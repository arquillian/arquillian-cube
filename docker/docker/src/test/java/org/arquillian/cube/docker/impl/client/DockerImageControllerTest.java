package org.arquillian.cube.docker.impl.client;


import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.Image;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.model.LocalCubeRegistry;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeConfiguration;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.AfterDestroy;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DockerImageControllerTest extends AbstractManagerTestBase {

    public static final String CUBE_ID = "pingpong";

    @Mock
    Cube cube;

    @Mock
    CubeContainer config;

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(DockerImageController.class);
        super.addExtensions(extensions);
    }

    @Before
    public void before(){
        when(cube.getId()).thenReturn(CUBE_ID);
        when(cube.configuration()).thenReturn(config);
        when(config.getImage()).thenReturn(new Image("jonmorehouse/ping-pong", null));
    }

    @Test
    public void should_remove_docker_image() {
        DockerClientExecutor executor = Mockito.mock(DockerClientExecutor.class);
        String config = "pingpong:\n" +
                "  image: jonmorehouse/ping-pong\n" +
                "  exposedPorts: [8080/tcp]\n" +
                "  portBindings: [8080->8080/tcp]\n";

        Map<String, String> parameters = new HashMap<>();
        parameters.put("dockerContainers", config);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(new HashMap<String, String>());
        bind(ApplicationScoped.class, CubeConfiguration.class, cubeConfiguration);

        CubeDockerConfiguration dockerConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        bind(ApplicationScoped.class, CubeDockerConfiguration.class, dockerConfiguration);

        final LocalCubeRegistry localCubeRegistry = new LocalCubeRegistry();
        localCubeRegistry.addCube(cube);
        bind(ApplicationScoped.class, CubeRegistry.class, localCubeRegistry);

        bind(ApplicationScoped.class, DockerClientExecutor.class, executor);

        fire(new AfterDestroy(CUBE_ID));
        Mockito.verify(executor).removeImage("jonmorehouse/ping-pong", false);
    }
}
