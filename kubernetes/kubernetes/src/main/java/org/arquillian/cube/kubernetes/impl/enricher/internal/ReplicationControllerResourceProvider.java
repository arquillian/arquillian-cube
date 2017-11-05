package org.arquillian.cube.kubernetes.impl.enricher.internal;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.v2_6.ReplicationController;
import io.fabric8.kubernetes.api.model.v2_6.ReplicationControllerList;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link io.fabric8.kubernetes.api.model.v2_6.ReplicationControllerList}.
 * It refers to replication controllers that have been created during the current session.
 */
public class ReplicationControllerResourceProvider extends AbstractKubernetesResourceProvider {
    @Override
    public boolean canProvide(Class<?> type) {
        return ReplicationController.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        String name = getName(qualifiers);
        if (name != null) {
            return getClient().replicationControllers()
                .inNamespace(getSession().getNamespace())
                .withName(getName(qualifiers))
                .get();
        }

        // Gets the first replication controller found that matches the labels.
        Map<String, String> labels = getLabels(qualifiers);
        ReplicationControllerList list = getClient().replicationControllers().inNamespace(getSession().getNamespace()).withLabels(labels).list();
        List<ReplicationController> replicationControllers = list.getItems();
        if( !replicationControllers.isEmpty() ) {
            return replicationControllers.get(0);
        }

        return null;
    }
}
