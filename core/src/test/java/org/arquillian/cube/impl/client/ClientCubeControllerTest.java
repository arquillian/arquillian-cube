package org.arquillian.cube.impl.client;

import java.util.HashMap;
import java.util.List;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.CubeID;
import org.arquillian.cube.impl.model.LocalCubeRegistry;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeConfiguration;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.CreateCube;
import org.arquillian.cube.spi.event.DestroyCube;
import org.arquillian.cube.spi.event.StartCube;
import org.arquillian.cube.spi.event.StopCube;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClientCubeControllerTest extends AbstractManagerTestBase {

    private static final String CUBE_ID = "x";
    private static final String MISSING_CUBE_ID = "y";
    @Inject
    private Instance<CubeController> controllerInst;
    @Mock
    private Cube<?> cube;

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(ClientCubeControllerCreator.class);
        super.addExtensions(extensions);
    }

    @Before
    public void setup() {
        when(cube.getId()).thenReturn(CUBE_ID);

        CubeRegistry registry = new LocalCubeRegistry();
        registry.addCube(cube);
        bind(ApplicationScoped.class, CubeRegistry.class, registry);

        CubeConfiguration configuration = CubeConfiguration.fromMap(new HashMap<String, String>());
        fire(configuration);
    }

    @Test
    public void shouldCreateWithCubeID() {
        controllerInst.get().create(new CubeID(CUBE_ID));
        assertEventFired(CreateCube.class, 1);
    }

    @Test
    public void shouldCreateWithCubeIdString() {
        controllerInst.get().create(CUBE_ID);
        assertEventFired(CreateCube.class, 1);
    }

    @Test
    public void shouldStartWithCubeID() {
        controllerInst.get().start(new CubeID(CUBE_ID));
        assertEventFired(StartCube.class, 1);
    }

    @Test
    public void shouldStartWithCubeIdString() {
        controllerInst.get().start(CUBE_ID);
        assertEventFired(StartCube.class, 1);
    }

    @Test
    public void shouldStopWithCubeID() {
        controllerInst.get().stop(new CubeID(CUBE_ID));
        assertEventFired(StopCube.class, 1);
    }

    @Test
    public void shouldStopWithCubeIdString() {
        controllerInst.get().stop(CUBE_ID);
        assertEventFired(StopCube.class, 1);
    }

    @Test
    public void shouldDestroyWithCubeID() {
        controllerInst.get().destroy(new CubeID(CUBE_ID));
        assertEventFired(DestroyCube.class, 1);
    }

    @Test
    public void shouldDestroyWithCubeIdString() {
        controllerInst.get().destroy(CUBE_ID);
        assertEventFired(DestroyCube.class, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailOnCreateWithMissingCubeId() {
        controllerInst.get().create(MISSING_CUBE_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailOnStartWithMissingCubeId() {
        controllerInst.get().start(MISSING_CUBE_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailOnStopWithMissingCubeId() {
        controllerInst.get().stop(MISSING_CUBE_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailOnDestroyWithMissingCubeId() {
        controllerInst.get().destroy(MISSING_CUBE_ID);
    }
}
