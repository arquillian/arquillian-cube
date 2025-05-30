package org.arquillian.cube.kubernetes.impl.enricher.internal;

import io.fabric8.openshift.client.OpenShiftClient;
import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;

import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * A {@link ResourceProvider} for {@link KubernetesClient}.
 */
public class ClientResourceProvider extends AbstractKubernetesResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return KubernetesClient.class.isAssignableFrom(type) && !OpenShiftClient.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        KubernetesClient client = getClient();

        if (client == null) {
            throw new IllegalStateException("Unable to inject Kubernetes client into test.");
        }
        return client;
    }

}
