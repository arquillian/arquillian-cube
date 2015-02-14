package org.arquillian.cube.impl.client;

import org.arquillian.cube.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.util.CommandLineExecutor;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeClientCreator {

    @Inject
    @ApplicationScoped
    private InstanceProducer<DockerClientExecutor> dockerClientExecutorProducer;

    public void createClient(@Observes CubeConfiguration cubeConfiguration) {
        dockerClientExecutorProducer.set(new DockerClientExecutor(cubeConfiguration, new CommandLineExecutor()));
    }
}
