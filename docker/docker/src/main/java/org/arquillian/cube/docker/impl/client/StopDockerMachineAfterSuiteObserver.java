package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.util.DockerMachine;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;

public class StopDockerMachineAfterSuiteObserver {

    //private static final Logger log = Logger.getLogger(StopDockerMachineAfterSuiteObserver.class.getName());

    @Inject
    private Instance<DockerMachine> dockerMachineInstance;

    @Inject
    private Instance<CubeDockerConfiguration> configurationProducer;

    public void stopDockerMachineIfStartedByCube(@Observes AfterSuite afterSuite) {

        if (dockerMachineInstance.get().isManuallyStarted()) {

            String machineName = configurationProducer.get().getMachineName();
            String cliPath = configurationProducer.get().getDockerMachinePath();

            dockerMachineInstance.get().stopDockerMachine(cliPath, machineName);
        }
    }

}
