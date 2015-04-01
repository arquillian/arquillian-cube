package org.arquillian.cube.impl.client;

import org.arquillian.cube.impl.util.Boot2Docker;
import org.arquillian.cube.impl.util.CommandLineExecutor;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.api.event.ManagerStarted;

public class Boot2DockerCreator {

    @Inject
    @ApplicationScoped
    private InstanceProducer<Boot2Docker> boot2DockerInstanceProducer;

    public void configure(@Observes(precedence = 100) ManagerStarted managerStarted) {
        boot2DockerInstanceProducer.set(new Boot2Docker(new CommandLineExecutor()));
    }
}
