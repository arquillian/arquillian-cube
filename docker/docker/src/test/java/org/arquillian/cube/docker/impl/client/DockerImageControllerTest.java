package org.arquillian.cube.docker.impl.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.model.LocalCubeRegistry;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeConfiguration;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.AfterDestroy;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DockerImageControllerTest extends AbstractManagerTestBase {

    public static final String CUBE_ID = "pingpong";

    @Mock
    Cube cube;

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(DockerImageController.class);
        super.addExtensions(extensions);
    }

    @Test
    public void should_remove_docker_image_if_built_by_cube() {
        DockerClientExecutor executor = Mockito.mock(DockerClientExecutor.class);
        String config = "pingpong:\n" +
            "  buildImage:\n" +
            "    dockerfileLocation: src/test/resources/tomcat\n" +
            "    noCache: true\n" +
            "    remove: true\n" +
            "  exposedPorts: [8080/tcp]\n" +
            "  portBindings: [8080->8080/tcp]\n";

        Map<String, String> parameters = new HashMap<>();
        parameters.put("dockerContainers", config);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(new HashMap<String, String>());
        bind(ApplicationScoped.class, CubeConfiguration.class, cubeConfiguration);

        CubeDockerConfiguration dockerConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        when(cube.getId()).thenReturn(CUBE_ID);
        when(cube.configuration()).thenReturn(dockerConfiguration.getDockerContainersContent().get(CUBE_ID));

        bind(ApplicationScoped.class, CubeDockerConfiguration.class, dockerConfiguration);

        final LocalCubeRegistry localCubeRegistry = new LocalCubeRegistry();
        localCubeRegistry.addCube(cube);
        bind(ApplicationScoped.class, CubeRegistry.class, localCubeRegistry);

        bind(ApplicationScoped.class, DockerClientExecutor.class, executor);

        fire(new AfterDestroy(CUBE_ID));
        Mockito.verify(executor).removeImage("pingpong:latest", false);
    }

    @Test
    public void should_not_remove_docker_image_as_not_built_by_cube() {
        DockerClientExecutor executor = Mockito.mock(DockerClientExecutor.class);

        String config = "pingpong:\n" +
            "  image: hashicorp/http-echo\n" +
            "  exposedPorts: [8080/tcp]\n" +
            "  portBindings: [8080->8080/tcp]\n";

        Map<String, String> parameters = new HashMap<>();
        parameters.put("dockerContainers", config);
        parameters.put("definitionFormat", DefinitionFormat.CUBE.name());

        CubeConfiguration cubeConfiguration = CubeConfiguration.fromMap(new HashMap<String, String>());
        bind(ApplicationScoped.class, CubeConfiguration.class, cubeConfiguration);

        CubeDockerConfiguration dockerConfiguration = CubeDockerConfiguration.fromMap(parameters, null);

        when(cube.getId()).thenReturn(CUBE_ID);
        when(cube.configuration()).thenReturn(dockerConfiguration.getDockerContainersContent().get(CUBE_ID));

        bind(ApplicationScoped.class, CubeDockerConfiguration.class, dockerConfiguration);

        final LocalCubeRegistry localCubeRegistry = new LocalCubeRegistry();
        localCubeRegistry.addCube(cube);
        bind(ApplicationScoped.class, CubeRegistry.class, localCubeRegistry);

        bind(ApplicationScoped.class, DockerClientExecutor.class, executor);

        fire(new AfterDestroy(CUBE_ID));
        Mockito.verify(executor, never()).removeImage("pingpong:latest", false);
    }
}
