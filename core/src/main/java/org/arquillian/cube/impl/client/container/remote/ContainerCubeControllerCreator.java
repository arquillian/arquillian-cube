package org.arquillian.cube.impl.client.container.remote;

import org.arquillian.cube.CubeController;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

public class ContainerCubeControllerCreator {

    @Inject
    @ApplicationScoped
    private InstanceProducer<CubeController> cubeController;

    @Inject
    private Instance<Injector> injector;

    public void createClientCubeController(@Observes BeforeSuite event) {
        cubeController.set(injector.get().inject(new ContainerCubeController()));
    }
}
