package org.arquillian.cube.kubernetes.impl.enricher.external;

import io.fabric8.kubernetes.api.model.v2_2.PodList;
import java.lang.annotation.Annotation;

import org.arquillian.cube.kubernetes.impl.enricher.AbstractKubernetesResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link PodList}.
 * It refers to pods that have been created during the current session.
 */
public class PodListResourceProvider extends AbstractKubernetesResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return internalToUserType(PodList.class.getName()).equals(type.getName());
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return toUsersResource(getClient().pods().inNamespace(getSession().getNamespace()).list());
    }
}
