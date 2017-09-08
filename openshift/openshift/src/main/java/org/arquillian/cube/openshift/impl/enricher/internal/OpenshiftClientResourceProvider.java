package org.arquillian.cube.openshift.impl.enricher.internal;

import io.fabric8.kubernetes.clnt.v2_6.KubernetesClient;
import io.fabric8.openshift.clnt.v2_6.OpenShiftClient;
import java.lang.annotation.Annotation;
import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link OpenShiftClient}.
 */
public class OpenshiftClientResourceProvider extends AbstractKubernetesResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return type.isAssignableFrom(OpenShiftClient.class);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        KubernetesClient client = getClient();

        if (client == null) {
            throw new IllegalStateException("Unable to inject Kubernetes client into test.");
        } else if (!client.isAdaptable(OpenShiftClient.class)) {
            throw new IllegalStateException("Could not adapt to OpenShiftClient.");
        }

        return client.adapt(OpenShiftClient.class);
    }
}
