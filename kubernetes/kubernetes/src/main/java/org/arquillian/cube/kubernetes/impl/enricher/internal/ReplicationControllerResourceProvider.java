package org.arquillian.cube.kubernetes.impl.enricher.internal;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;

import io.fabric8.kubernetes.api.model.v2_2.ReplicationController;

/**
 * A {@link ResourceProvider} for {@link io.fabric8.kubernetes.api.model.v2_2.ReplicationControllerList}.
 * It refers to replication controllers that have been created during the current session.
 */
public class ReplicationControllerResourceProvider extends AbstractKubernetesResourceProvider {
    @Override
    public boolean canProvide(Class<?> type) {
        return ReplicationController.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return getClient().replicationControllers()
            .inNamespace(getSession().getNamespace())
            .withName(getName(qualifiers))
            .get();
    }
}
