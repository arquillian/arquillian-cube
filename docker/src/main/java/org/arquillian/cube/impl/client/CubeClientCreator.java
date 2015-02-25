package org.arquillian.cube.impl.client;

import org.arquillian.cube.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.util.Boot2Docker;
import org.arquillian.cube.impl.util.OperatingSystemResolver;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CubeClientCreator {

    @Inject
    @ApplicationScoped
    private InstanceProducer<DockerClientExecutor> dockerClientExecutorProducer;

    @Inject
    private Instance<Boot2Docker> boot2DockerInstance;
    
    public void createClient(@Observes CubeConfiguration cubeConfiguration) {
        dockerClientExecutorProducer.set(new DockerClientExecutor(cubeConfiguration, boot2DockerInstance.get(),
                new OperatingSystemResolver()));
    }
}
