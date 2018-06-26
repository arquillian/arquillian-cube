package org.arquillian.cube.kubernetes.impl.enricher.internal;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.v4_0.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.v4_0.apps.ReplicaSetList;

/**
 * A {@link ResourceProvider} for {@link ReplicaSet}.
 * It refers to replica sets that have been created during the current session.
 */
public class ReplicaSetResourceProvider extends AbstractKubernetesResourceProvider {
    @Override
    public boolean canProvide(Class<?> type) {
        return ReplicaSet.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        String name = getName(qualifiers);
        String namespace = getNamespace(qualifiers);
        if (name != null) {
            return getClient().extensions()
                .replicaSets()
                .inNamespace(namespace)
                .withName(name)
                .get();
        }

        // Gets the first replica set found that matches the labels.
        Map<String, String> labels = getLabels(qualifiers);
        ReplicaSetList list = getClient().extensions().replicaSets().inNamespace(namespace).withLabels(labels).list();
        List<ReplicaSet> replicaSets = list.getItems();
        if( !replicaSets.isEmpty() ) {
            return replicaSets.get(0);
        }

        return null;
    }
}
