package org.arquillian.cube.kubernetes.impl.enricher.internal;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;

import io.fabric8.kubernetes.api.model.v2_2.extensions.ReplicaSetList;

/**
 * A {@link ResourceProvider} for {@link ReplicaSetList}.
 * It refers to replica sets that have been created during the current session.
 */
public class ReplicaSetListResourceProvider extends AbstractKubernetesResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return ReplicaSetList.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return getClient().extensions().replicaSets().inNamespace(getSession().getNamespace()).list();
    }
}
