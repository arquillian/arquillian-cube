package org.arquillian.cube.kubernetes.impl.enricher;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.kubernetes.impl.DefaultSession;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;

/**
 * Created by iocanel on 7/30/16.
 */
public abstract class AbstractKubernetesResourceProvider implements ResourceProvider {

    @Inject
    private Instance<KubernetesClient> client;

    @Inject
    private Instance<DefaultSession> seesion;

    protected String getName(Annotation... qualifiers) {
        for (Annotation annotation : qualifiers) {
            if (annotation instanceof Named) {
                return ((Named) annotation).value();
            }
        }
        return null;
    }

    protected KubernetesClient getClient() {
        return client.get();
    }

    public DefaultSession getSession() {
        return seesion.get();
    }
}
