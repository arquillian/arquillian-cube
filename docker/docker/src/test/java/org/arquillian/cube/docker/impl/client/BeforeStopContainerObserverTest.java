package org.arquillian.cube.docker.impl.client;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.DockerCompositions;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.docker.impl.util.ConfigUtil;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.BeforeStop;
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
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class BeforeStopContainerObserverTest extends AbstractManagerTestBase {

    private static final String CUBE_CONTAINER_NAME = "test";

    private static final String CONTAINER_COPY_CONFIGURATION =
        "tomcat_default:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  beforeStop:\n" +
            "    - copy:\n" +
            "        from: /test\n" +
            "        to: ";

    private static final String CONTAINER_LOG_CONFIGURATION =
        "tomcat_default:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  beforeStop:\n" +
            "    - log:\n" +
            "        to: ";

    private static final String CONTAINER_CUSTOM_BEFORE_STOP_ACTION_CONFIGURATION =
        "tomcat_default:\n" +
            "  image: tutum/tomcat:7.0\n" +
            "  beforeStop:\n" +
            "    - customBeforeStopAction:\n" +
            "        strategy: org.arquillian.cube.docker.impl.beforeStop.CustomBeforeStopActionImpl";

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
        extensions.add(BeforeStopContainerObserver.class);
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
    public void shouldCopyFileFromContainer() throws IOException {
        File newFolder = temporaryFolder.newFolder();
        String content = CONTAINER_COPY_CONFIGURATION;
        content += newFolder.getAbsolutePath();

        DockerCompositions configuration = ConfigUtil.load(content);
        CubeContainer config = configuration.get("tomcat_default");

        Mockito.when(cube.configuration()).thenReturn(config);
        Mockito.when(dockerClientExecutor.getFileOrDirectoryFromContainerAsTar(eq(CUBE_CONTAINER_NAME), anyString()))
            .thenReturn(BeforeStopContainerObserverTest.class.getResourceAsStream("/hello.tar"));
        fire(new BeforeStop(CUBE_CONTAINER_NAME));
        verify(dockerClientExecutor).getFileOrDirectoryFromContainerAsTar(eq(CUBE_CONTAINER_NAME), eq("/test"));
        assertThat(new File(newFolder, "hello.txt").exists(), is(true));
    }

    @Test
    public void shouldGetLogFromContainer() throws IOException {
        File newFolder = temporaryFolder.newFolder();
        String content = CONTAINER_LOG_CONFIGURATION;
        content += newFolder.getAbsolutePath();
        content += "mylog.log";

        DockerCompositions configuration = ConfigUtil.load(content);
        CubeContainer config = configuration.get("tomcat_default");
        Mockito.when(cube.configuration()).thenReturn(config);
        fire(new BeforeStop(CUBE_CONTAINER_NAME));
        verify(dockerClientExecutor, times(1)).copyLog(eq(CUBE_CONTAINER_NAME), eq(false), eq(false), eq(false),
            eq(false), eq(-1), any(OutputStream.class));
    }

    @Test
    public void shouldExecuteCustomBeforeStopOperationForContainer() throws IOException {
        DockerCompositions configuration = ConfigUtil.load(CONTAINER_CUSTOM_BEFORE_STOP_ACTION_CONFIGURATION);
        CubeContainer config = configuration.get("tomcat_default");
        Mockito.when(cube.configuration()).thenReturn(config);
        fire(new BeforeStop(CUBE_CONTAINER_NAME));
        verify(dockerClientExecutor, times(1)).getDockerUri();
    }
}

