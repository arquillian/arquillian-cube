package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.util.Top;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.api.event.ManagerStarted;

public class TopCreator {

    @Inject
    @ApplicationScoped
    private InstanceProducer<Top> topInstanceProducer;

    public void configure(@Observes(precedence = 100) ManagerStarted managerStarted) {
        topInstanceProducer.set(new Top());
    }
}
