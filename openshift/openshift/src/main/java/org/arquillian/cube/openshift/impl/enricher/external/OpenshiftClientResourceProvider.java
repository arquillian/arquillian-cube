package org.arquillian.cube.openshift.impl.enricher.external;

import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import java.util.logging.Logger;
import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;

/**
 * A {@link ResourceProvider} for {@link OpenShiftClient}.
 */
public class OpenshiftClientResourceProvider extends AbstractKubernetesResourceProvider {
    protected final Logger log = Logger.getLogger(getClass().getName());

    @Override
    public boolean canProvide(Class<?> type) {
        log.info("Executing OpenShiftClient::canProvide...");
        return internalToUserType(OpenShiftClient.class.getName()).equals(type.getName());
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        log.info("OpenShiftClient is looking up for resources...");
        KubernetesClient client = getClient();

        if (client == null) {
            throw new IllegalStateException("Unable to inject Kubernetes client into test.");
        } else if (!client.supports(Project.class)) {
            throw new IllegalStateException("Could not adapt to OpenShiftClient.");
        }

        return createUserClient(client.adapt(OpenShiftClient.class));
    }

    private Object createUserClient(OpenShiftClient client) {
        log.info("Creating OpenShiftClient...");
        Config config = client.getConfiguration();

        Object userConfig = toUsersResource(config);
        Class userConfigClass = loadClass(internalToUserType(config.getClass().getName()));
        Class userClientClass = loadClass(internalToUserType(DefaultOpenShiftClient.class.getName()));
        try {
            Constructor<?> constructor = userClientClass.getConstructor(userConfigClass);
            return constructor.newInstance(userConfig);
        } catch (Throwable t) {
            return null;
        }
    }
}
