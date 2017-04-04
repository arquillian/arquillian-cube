package org.arquillian.cube.kubernetes.impl.enricher.external;

import io.fabric8.kubernetes.api.model.v2_2.extensions.ReplicaSetList;
import java.lang.annotation.Annotation;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link io.fabric8.kubernetes.api.model.v2_2.extensions.ReplicaSetList}.
 * It refers to replica sets that have been created during the current session.
 */
public class ReplicaSetListResourceProvider extends AbstractKubernetesResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return internalToUserType(ReplicaSetList.class.getName()).equals(type.getName());
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return toUsersResource(getClient().extensions().replicaSets().inNamespace(getSession().getNamespace()).list());
    }
}
