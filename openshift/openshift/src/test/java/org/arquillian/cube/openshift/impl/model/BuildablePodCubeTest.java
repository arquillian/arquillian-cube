package org.arquillian.cube.openshift.impl.model;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteList;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.arquillian.cube.openshift.impl.client.OpenShiftClient;
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
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.arquillian.cube.openshift.impl.client.OpenShiftClient.ResourceHolder;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BuildablePodCubeTest extends AbstractManagerTestBase {

    @Mock
    private CubeOpenShiftConfiguration cubeOpenShiftConfiguration;

    @Mock
    private OpenShiftClient openShiftClient;

    @Mock
    private io.fabric8.openshift.client.OpenShiftClient openShiftClientExt;

    @Mock
    private MixedOperation<Route, RouteList, Resource<Route>> routeOperations;

    @Inject
    private Instance<Injector> injectorInst;

    private BuildablePodCube buildablePodCube;

    @Before
    public void setup() throws Exception {

        final Pod pod = new Pod("v1", "Pod", new ObjectMeta(), new PodSpec(), null);
        final ResourceHolder resourceHolder = new ResourceHolder(pod);
        when(openShiftClient.build(ArgumentMatchers.any())).thenReturn(resourceHolder);

        when(openShiftClient.getClientExt()).thenReturn(openShiftClientExt);
        when(openShiftClientExt.routes()).thenReturn(routeOperations);
        when(routeOperations.list()).thenReturn(new RouteList());

        when(cubeOpenShiftConfiguration.isNamespaceCleanupEnabled()).thenReturn(true);

        buildablePodCube = injectorInst.get().inject(new BuildablePodCube(pod, openShiftClient, cubeOpenShiftConfiguration));
        buildablePodCube.holder = resourceHolder;
    }

    @Test
    public void shouldFireLifecycleEventsDuringCreate() {
        buildablePodCube.create();
        assertEventFired(BeforeCreate.class, 1);
        assertEventFired(AfterCreate.class, 1);
    }

    @Test
    public void shouldFireLifecycleEventsDuringCreateAfterDestroyed() {
        // given calling entire lifecycle to destroy buildablePodCube
        buildablePodCube.create();
        buildablePodCube.start();
        buildablePodCube.stop();
        buildablePodCube.destroy();

        // when
        buildablePodCube.create();

        // then event count is 2 which is for two buildablePodCube.create()
        assertEventFired(BeforeCreate.class, 2);
        assertEventFired(AfterCreate.class, 2);
    }

    @Test
    public void shouldFireLifecycleEventsDuringStart() {
        buildablePodCube.start();
        assertEventFired(BeforeStart.class, 1);
        assertEventFired(AfterStart.class, 1);
    }

    @Test
    public void shouldFireLifecycleEventsDuringStop() {
        buildablePodCube.stop();
        assertEventFired(BeforeStop.class, 1);
        assertEventFired(AfterStop.class, 1);
    }

    @Test
    public void shouldFireLifecycleEventsDuringDestroy() {
        buildablePodCube.stop(); // require a stopped buildablePodCube to destroy it.
        buildablePodCube.destroy();
        assertEventFired(BeforeDestroy.class, 1);
        assertEventFired(AfterDestroy.class, 1);
    }

    @Test
    public void shouldNotFireLifecycleEventsIfTryingToStopAlreadyDestroyedCube() {
        // given
        buildablePodCube.stop();
        buildablePodCube.destroy();

        // when
        buildablePodCube.stop();

        // then - event count is 1 which is for first buildablePodCube.stop()
        assertEventFired(BeforeStop.class, 1);
        assertEventFired(AfterStop.class, 1);
    }

    @Test
    public void shouldNotFireLifecycleEventsIfTryingToStopAlreadyStoppedCube() {
        // given
        buildablePodCube.stop();

        // when
        buildablePodCube.stop();

        // then  - event count is 1 which is for first buildablePodCube.stop()
        assertEventFired(BeforeStop.class, 1);
        assertEventFired(AfterStop.class, 1);
    }
}

