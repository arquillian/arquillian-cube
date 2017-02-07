package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.ContainerObjectFactory;
import org.arquillian.cube.docker.impl.client.containerobject.DockerContainerObjectFactory;
import org.arquillian.cube.spi.CubeConfiguration;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class ContainerObjectFactoryRegistrar {

    @Inject
    @ApplicationScoped
    private InstanceProducer<ContainerObjectFactory> producer;

    @Inject
    private Instance<Injector> injector;

    public void createClientCubeController(@Observes CubeConfiguration event) {
        producer.set(injector.get().inject(new DockerContainerObjectFactory()));
    }
}
