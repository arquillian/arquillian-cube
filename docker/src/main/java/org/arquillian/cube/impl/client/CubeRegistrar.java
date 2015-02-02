package org.arquillian.cube.impl.client;

import java.util.Map;

import org.arquillian.cube.impl.client.name.NameGenerator;
import org.arquillian.cube.impl.client.name.NameGeneratorFactory;
import org.arquillian.cube.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.model.DockerCube;
import org.arquillian.cube.impl.model.DockerCubeRegistry;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeRegistrar {

    @Inject @ApplicationScoped
    private InstanceProducer<CubeRegistry> registryProducer;

    @SuppressWarnings("unchecked")
    public void register(@Observes DockerClientExecutor executor, CubeConfiguration configuration, Injector injector) {
        final NameGenerator nameGenerator = NameGeneratorFactory.getGenerator( configuration );

        DockerCubeRegistry registry = new DockerCubeRegistry();

        //TODO, add key here generation here
        Map<String, Object> containerConfigurations = configuration.getDockerContainersContent();


        for(Map.Entry<String, Object> containerConfiguration : containerConfigurations.entrySet()) {



            registry.addCube(
                    injector.inject(
                        new DockerCube(
                                containerConfiguration.getKey(),
                                (Map<String, Object>)containerConfiguration.getValue(),
                                executor, nameGenerator)));
        }

        registryProducer.set(registry);
    }
}
