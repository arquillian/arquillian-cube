package org.arquillian.cube.kubernetes.impl.enricher;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;

/**
 * A {@link ResourceProvider} for {@link io.fabric8.kubernetes.client.KubernetesClient}.
 */
public class ClientResourceProvider extends AbstractKubernetesResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return type.isAssignableFrom(KubernetesClient.class);
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
