package org.arquillian.cube.kubernetes.impl.enricher.external;

import java.lang.annotation.Annotation;

import io.fabric8.kubernetes.api.model.v2_6.PodList;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * A {@link ResourceProvider} for {@link PodList}.
 * It refers to pods that have been created during the current session.
 */
public class PodListResourceProvider extends org.arquillian.cube.kubernetes.impl.enricher.internal.PodListResourceProvider {

    @Override
    public boolean canProvide(Class<?> type) {
        return internalToUserType(PodList.class.getName()).equals(type.getName());
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return toUsersResource(super.lookup(resource, qualifiers));
    }
}
