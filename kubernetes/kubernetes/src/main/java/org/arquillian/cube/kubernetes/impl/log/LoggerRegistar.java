package org.arquillian.cube.kubernetes.impl.log;

import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.Logger;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class LoggerRegistar {

    @Inject
    @ApplicationScoped
    private InstanceProducer<Logger> logger;

    public void createLogger(@Observes Configuration configuration) {
        if (configuration.isAnsiLoggerEnabled()) {
            logger.set(new AnsiLogger());
        } else {
            logger.set(new SimpleLogger());
        }
    }
}
