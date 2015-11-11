package org.arquillian.cube.impl.client;

import org.arquillian.cube.CubeController;
import org.arquillian.cube.spi.CubeConfiguration;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class ClientCubeControllerCreator {

    @Inject
    @ApplicationScoped
    private InstanceProducer<CubeController> cubeController;

    @Inject
    private Instance<Injector> injector;

    public void createClientCubeController(@Observes CubeConfiguration event) {
        cubeController.set(injector.get().inject(new ClientCubeController()));
    }
}
