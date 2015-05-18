package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class DockerClientCreator {

    @Inject
    @ApplicationScoped
    private InstanceProducer<DockerClientExecutor> dockerClientExecutorProducer;

    public void createClient(@Observes CubeDockerConfiguration cubeConfiguration) {
        dockerClientExecutorProducer.set(new DockerClientExecutor(cubeConfiguration));
    }
}
