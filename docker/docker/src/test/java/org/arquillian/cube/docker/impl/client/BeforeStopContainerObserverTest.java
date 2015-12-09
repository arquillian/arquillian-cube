package org.arquillian.cube.docker.impl.client;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.arquillian.cube.docker.impl.client.BeforeStopContainerObserver;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Cube;
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
import org.yaml.snakeyaml.Yaml;

@RunWith(MockitoJUnitRunner.class)
public class BeforeStopContainerObserverTest extends AbstractManagerTestBase {


    private static final String CUBE_CONTAINER_NAME = "test";

    private static final String CONTAINER_COPY_CONFIGURATION =
        "tomcat_default:\n" + "  image: tutum/tomcat:7.0\n"
            + "  beforeStop:\n" + "    - copy:\n"
            + "        from: /test\n" + "        to: ";

    private static final String CONTAINER_LOG_CONFIGURATION =
        "tomcat_default:\n" + "  image: tutum/tomcat:7.0\n"
            + "  beforeStop:\n" + "    - log:\n" + "        to: ";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private Cube cube;

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

        Mockito.when(registry.getCube(CUBE_CONTAINER_NAME)).thenReturn(
            cube);
        bind(ApplicationScoped.class, CubeRegistry.class, registry);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCopyFileFromContainer() throws IOException {
        File newFolder = temporaryFolder.newFolder();
        String content = CONTAINER_COPY_CONFIGURATION;
        content += newFolder.getAbsolutePath();

        Map<String, Object> configuration =
            (Map<String, Object>) new Yaml().load(content);
        Map<String, Object> config =
            (Map<String, Object>) configuration.get("tomcat_default");
        Mockito.when(cube.configuration()).thenReturn(config);
        Mockito.when(dockerClientExecutor.getFileOrDirectoryFromContainerAsTar(eq(CUBE_CONTAINER_NAME), anyString())).thenReturn(BeforeStopContainerObserverTest.class.getResourceAsStream("/hello.tar"));
        fire(new BeforeStop(CUBE_CONTAINER_NAME));
        verify(dockerClientExecutor).getFileOrDirectoryFromContainerAsTar(eq(CUBE_CONTAINER_NAME), eq("/test"));
        assertThat(new File(newFolder, "hello.txt").exists(), is(true));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldGetLogFromContainer() throws IOException {
        File newFolder = temporaryFolder.newFolder();
        String content = CONTAINER_LOG_CONFIGURATION;
        content += newFolder.getAbsolutePath();
        content += "mylog.log";

        Map<String, Object> configuration =
            (Map<String, Object>) new Yaml().load(content);
        Map<String, Object> config =
            (Map<String, Object>) configuration.get("tomcat_default");
        Mockito.when(cube.configuration()).thenReturn(config);
        fire(new BeforeStop(CUBE_CONTAINER_NAME));
        verify(dockerClientExecutor, times(1)).copyLog(eq(CUBE_CONTAINER_NAME), eq(false), eq(false), eq(false), eq(false), eq(-1), any(OutputStream.class));
    }
}
