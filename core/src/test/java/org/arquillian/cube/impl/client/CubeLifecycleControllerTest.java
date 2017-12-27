package org.arquillian.cube.impl.client;

import java.util.List;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.impl.model.LocalCubeRegistry;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.CreateCube;
import org.arquillian.cube.spi.event.DestroyCube;
import org.arquillian.cube.spi.event.StartCube;
import org.arquillian.cube.spi.event.StopCube;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Category({RequiresDocker.class, RequiresDockerMachine.class})
public class CubeLifecycleControllerTest extends AbstractManagerTestBase {

    public static final String CUBE_ID = "test";
    public static final String MISSING_CUBE_ID = "_MISSING_";
    public CubeRegistry cubeRegistry;
    @Mock
    public Cube<?> cube;

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(CubeLifecycleController.class);
        super.addExtensions(extensions);
    }

    @Before
    public void setup() {
        when(cube.getId()).thenReturn(CUBE_ID);
        cubeRegistry = new LocalCubeRegistry();
        cubeRegistry.addCube(cube);

        bind(ApplicationScoped.class, CubeRegistry.class, cubeRegistry);
    }

    @Test
    public void shouldCreateCube() {
        fire(new CreateCube(CUBE_ID));
        verify(cube).create();
    }

    @Test
    public void shouldStartCube() {
        fire(new StartCube(CUBE_ID));
        verify(cube).start();
    }

    @Test
    public void shouldStopCube() {
        fire(new StopCube(CUBE_ID));
        verify(cube).stop();
    }

    @Test
    public void shouldDestroyCube() {
        fire(new DestroyCube(CUBE_ID));
        verify(cube).destroy();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionOnMisingCubeDuringCreateCube() {
        fire(new CreateCube(MISSING_CUBE_ID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionOnMisingCubeDuringStartCube() {
        fire(new StartCube(MISSING_CUBE_ID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionOnMisingCubeDuringStopCube() {
        fire(new StopCube(MISSING_CUBE_ID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionOnMisingCubeDuringDestroyCube() {
        fire(new DestroyCube(MISSING_CUBE_ID));
    }
}
