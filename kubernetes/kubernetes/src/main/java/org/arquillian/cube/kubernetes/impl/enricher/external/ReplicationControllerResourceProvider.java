package org.arquillian.cube.kubernetes.impl.enricher.external;

import io.fabric8.kubernetes.api.model.v2_2.ReplicationController;
import java.lang.annotation.Annotation;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link io.fabric8.kubernetes.api.model.v2_2.ReplicationControllerList}.
 * It refers to replication controllers that have been created during the current session.
 */
public class ReplicationControllerResourceProvider extends AbstractKubernetesResourceProvider {
    @Override
    public boolean canProvide(Class<?> type) {
        return internalToUserType(ReplicationController.class.getName()).equals(type.getName());
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return toUsersResource(getClient().replicationControllers()
            .inNamespace(getSession().getNamespace())
            .withName(getName(qualifiers))
            .get());
    }
}
