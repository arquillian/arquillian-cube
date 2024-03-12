package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.docker.impl.util.ConfigUtil;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.AfterStart;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AfterStartContainerObserverTest extends AbstractManagerTestBase {

    private static final String CUBE_CONTAINER_NAME = "test";

    private static final String CONTAINER_COPY_CONFIGURATION =
        "tomcat_default:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  afterStart:\n" +
            "    - copy:\n" +
            "        to: /test\n" +
            "        from: ";

    private static final String CONTAINER_CUSTOM_AFTER_START_ACTION_CONFIGURATION =
        "tomcat_default:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  afterStart:\n" +
            "    - customAfterStartAction:\n" +
            "        strategy: org.arquillian.cube.docker.impl.afterStart.CustomAfterStartActionImpl";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private DockerCube cube;

    @Mock
    private CubeRegistry registry;

    @Mock
    private DockerClientExecutor dockerClientExecutor;

    @BeforeClass
    public static void startDockerStub() {
    }

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        super.addExtensions(extensions);
        extensions.add(AfterStartContainerObserver.class);
    }

    @Before
    public void setup() {
        bind(ApplicationScoped.class, DockerClientExecutor.class,
            dockerClientExecutor);

        Mockito.when(registry.getCube(CUBE_CONTAINER_NAME, DockerCube.class)).thenReturn(
            cube);
        bind(ApplicationScoped.class, CubeRegistry.class, registry);
    }

    @Test
    public void shouldCopyFileToContainer() throws IOException {
        File newFolder = temporaryFolder.newFolder();
        String content = CONTAINER_COPY_CONFIGURATION;
        content += newFolder.getAbsolutePath();

        DockerCompositions configuration = ConfigUtil.load(content);
        CubeContainer config = configuration.get("tomcat_default");

        Mockito.when(cube.configuration()).thenReturn(config);
        fire(new AfterStart(CUBE_CONTAINER_NAME));
        verify(dockerClientExecutor).copyStreamToContainer(eq(CUBE_CONTAINER_NAME), eq(newFolder), eq("/test"));
    }

    @Test
    public void shouldExecuteCustomAfterStartOperationForContainer() throws IOException {
        DockerCompositions configuration = ConfigUtil.load(CONTAINER_CUSTOM_AFTER_START_ACTION_CONFIGURATION);
        CubeContainer config = configuration.get("tomcat_default");
        Mockito.when(cube.configuration()).thenReturn(config);
        fire(new AfterStart(CUBE_CONTAINER_NAME));
        verify(dockerClientExecutor, times(1)).getDockerUri();
    }
}

