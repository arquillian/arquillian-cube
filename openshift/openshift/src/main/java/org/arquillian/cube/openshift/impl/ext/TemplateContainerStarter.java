package org.arquillian.cube.openshift.impl.ext;

import java.util.List;
import java.util.logging.Logger;
import org.arquillian.cube.openshift.api.model.DeploymentConfig;
import org.arquillian.cube.openshift.api.model.OpenShiftResource;
import org.arquillian.cube.openshift.impl.CEEnvironmentProcessor;
import org.arquillian.cube.openshift.impl.adapter.OpenShiftAdapter;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.arquillian.cube.openshift.impl.client.OpenShiftClient;
import org.arquillian.cube.openshift.impl.utils.Operator;
import org.jboss.arquillian.container.spi.event.container.AfterStart;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;

public class TemplateContainerStarter {

    private final Logger log = Logger.getLogger(TemplateContainerStarter.class.getName());

    /**
     * Wait for the template resources to come up after the test container has
     * been started. This allows the test container and the template resources
     * to come up in parallel.
     */
    public void waitForDeployments(@Observes(precedence = -100) AfterStart event, OpenShiftAdapter client,
        CEEnvironmentProcessor.TemplateDetails details, TestClass testClass, CubeOpenShiftConfiguration configuration, OpenShiftClient openshiftClient)
        throws Exception {
        if (testClass == null) {
            // nothing to do, since we're not in ClassScoped context
            return;
        }
        if (details == null) {
            log.warning(String.format("No environment for %s", testClass.getName()));
            return;
        }
        log.info(String.format("Waiting for environment for %s", testClass.getName()));
        try {
            for (List<? extends OpenShiftResource> resources : details.getResources()) {
                delay(client, resources);
            }
        } catch (Throwable t) {
            throw new IllegalArgumentException("Error waiting for template resources to deploy: " + testClass.getName(), t);
        }
    }

    private void delay(OpenShiftAdapter client, final List<? extends OpenShiftResource> resources) throws Exception {
        for (OpenShiftResource resource : resources) {
            if (resource instanceof DeploymentConfig) {
                final DeploymentConfig dc = (DeploymentConfig) resource;
                client.delay(dc.getSelector(), dc.getReplicas(), Operator.EQUAL);
            }
        }
    }

}
