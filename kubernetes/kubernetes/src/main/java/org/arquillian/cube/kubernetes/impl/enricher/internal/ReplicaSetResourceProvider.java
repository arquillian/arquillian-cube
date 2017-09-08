package org.arquillian.cube.kubernetes.impl.enricher.internal;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.v2_6.extensions.ReplicaSet;
import io.fabric8.kubernetes.api.model.v2_6.extensions.ReplicaSetList;

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
        if (name != null) {
            return getClient().extensions()
                .replicaSets()
                .inNamespace(getSession().getNamespace())
                .withName(getName(qualifiers))
                .get();
        }

        // Gets the first replica set found that matches the labels.
        Map<String, String> labels = getLabels(qualifiers);
        ReplicaSetList list = getClient().extensions().replicaSets().inNamespace(getSession().getNamespace()).withLabels(labels).list();
        List<ReplicaSet> replicaSets = list.getItems();
        if( !replicaSets.isEmpty() ) {
            return replicaSets.get(0);
        }

        return null;
    }
}
