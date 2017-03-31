package org.arquillian.cube.docker.impl.client;


import org.arquillian.cube.docker.impl.util.CommandLineExecutor;
import org.arquillian.cube.docker.impl.util.DockerMachine;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.api.event.ManagerStarted;

public class DockerMachineCreator {

    @Inject
    @ApplicationScoped
    private InstanceProducer<DockerMachine> dockerMachineInstanceProducer;

    public void configure(@Observes(precedence = 100) ManagerStarted managerStarted) {
        dockerMachineInstanceProducer.set(new DockerMachine(new CommandLineExecutor()));
    }

}
