package org.arquillian.cube.impl.model;

import java.util.HashMap;

import org.arquillian.cube.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.event.lifecycle.AfterCreate;
import org.arquillian.cube.spi.event.lifecycle.AfterDestroy;
import org.arquillian.cube.spi.event.lifecycle.AfterStart;
import org.arquillian.cube.spi.event.lifecycle.AfterStop;
import org.arquillian.cube.spi.event.lifecycle.BeforeCreate;
import org.arquillian.cube.spi.event.lifecycle.BeforeDestroy;
import org.arquillian.cube.spi.event.lifecycle.BeforeStart;
import org.arquillian.cube.spi.event.lifecycle.BeforeStop;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DockerCubeTest extends AbstractManagerTestBase {

    @Mock
    private DockerClientExecutor executor;

    @Inject
    private Instance<Injector> injectorInst;

    private DockerCube cube;

    @Before
    public void setup() {
        cube = injectorInst.get().inject(new DockerCube("test", new HashMap<String, Object>(), executor));
    }

    @Test
    public void shouldFireLifecycleEventsDuringCreate() {
        cube.create();
        assertEventFired(BeforeCreate.class, 1);
        assertEventFired(AfterCreate.class, 1);
    }

    @Test
    public void shouldFireLifecycleEventsDuringStart() {
        cube.start();
        assertEventFired(BeforeStart.class, 1);
        assertEventFired(AfterStart.class, 1);
    }

    @Test
    public void shouldFireLifecycleEventsDuringStop() {
        cube.stop();
        assertEventFired(BeforeStop.class, 1);
        assertEventFired(AfterStop.class, 1);
    }

    @Test
    public void shouldFireLifecycleEventsDuringDestroy() {
        cube.stop(); // require a stopped Cube to destroy it.
        cube.destroy();
        assertEventFired(BeforeDestroy.class, 1);
        assertEventFired(AfterDestroy.class, 1);
    }
}
