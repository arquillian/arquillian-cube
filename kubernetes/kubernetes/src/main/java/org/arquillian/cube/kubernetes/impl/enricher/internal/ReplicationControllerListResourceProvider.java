package org.arquillian.cube.kubernetes.impl.enricher.internal;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;

import io.fabric8.kubernetes.api.model.v2_2.ReplicationControllerList;

/**
 * A {@link ResourceProvider} for {@link ReplicationControllerList}.
 * It refers to replication controllers that have been created during the current session.
 */
public class ReplicationControllerListResourceProvider extends AbstractKubernetesResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return ReplicationControllerList.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return getClient().replicationControllers().inNamespace(getSession().getNamespace()).list();
    }
}
